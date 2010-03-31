/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.submission;

import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.util.ConnectionResult;
import org.orbeon.oxf.util.NetUtils;
import org.orbeon.oxf.util.PropertyContext;
import org.orbeon.oxf.xforms.XFormsContainingDocument;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Handle replace="all".
 */
public class AllReplacer extends BaseReplacer {

    public AllReplacer(XFormsModelSubmission submission, XFormsContainingDocument containingDocument) {
        super(submission, containingDocument);
    }

    public void deserialize(PropertyContext propertyContext, ConnectionResult connectionResult, XFormsModelSubmission.SubmissionParameters p, XFormsModelSubmission.SecondPassParameters p2) {
        // NOP
    }

    public Runnable replace(PropertyContext propertyContext, ConnectionResult connectionResult, XFormsModelSubmission.SubmissionParameters p, XFormsModelSubmission.SecondPassParameters p2) throws IOException {

        // When we get here, we are in a mode where we need to send the reply directly to an external context, if any.

        // Remember that we got a submission producing output
        containingDocument.setGotSubmissionReplaceAll();

        // Get response from containing document
        final ExternalContext.Response response = containingDocument.getResponse();

        // Set content-type
        response.setContentType(connectionResult.getResponseContentType());

        // Forward headers to response
        connectionResult.forwardHeaders(response);

        // Forward content to response
        final OutputStream outputStream = response.getOutputStream();
        NetUtils.copyStream(connectionResult.getResponseInputStream(), outputStream);

        // End document and close
        outputStream.flush();
        outputStream.close();

        // TODO: [#306918] RFE: Must be able to do replace="all" during initialization.
        // http://forge.objectweb.org/tracker/index.php?func=detail&aid=306918&group_id=168&atid=350207
        // Suggestion is to write either binary or XML to processor output ContentHandler,
        // and make sure the code which would output the XHTML+XForms is disabled.

        // "the event xforms-submit-done may be dispatched"
        // we don't want any changes to happen to the document upon xxforms-submit when producing a new document
        if (!p.isDeferredSubmissionSecondPassReplaceAll) {
            return dispatchSubmitDone(propertyContext, connectionResult);
        } else {
            return null;
        }
    }
}
