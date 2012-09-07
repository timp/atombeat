
package org.atombeat.xquery.functions.fn;

import java.text.Collator;

import org.apache.log4j.Logger;
import org.atombeat.value.Whitespace;
import org.atombeat.xquery.functions.util.AtomBeatUtilModule;
import org.exist.Namespaces;
import org.exist.dom.NodeProxy;
import org.exist.dom.QName;
import org.exist.memtree.NodeImpl;
import org.exist.memtree.ReferenceNode;
import org.exist.xquery.Cardinality;
import org.exist.xquery.Constants;
import org.exist.xquery.Dependency;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Profiler;
import org.exist.xquery.ValueComparison;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.CollatingFunction;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.NumericValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Implements the fn:deep-equal library function.
 *
 * @author <a href="mailto:piotr@ideanest.com">Piotr Kaminski</a>
 */
public class FunDeepEqual extends CollatingFunction {

	/**
	 * Flag indicating that comment children are taken into account when
	 * comparing element or document nodes
	 */
	public static final int INCLUDE_COMMENTS = 1 << 2;

	/**
	 * Flag indicating that processing instruction nodes are taken into account
	 * when comparing element or document nodes
	 */
	public static final int INCLUDE_PROCESSING_INSTRUCTIONS = 1 << 3;

	/**
	 * Flag indicating that whitespace text nodes are ignored when comparing
	 * element nodes
	 */
	public static final int EXCLUDE_WHITESPACE_TEXT_NODES = 1 << 4;

    protected static final Logger logger = Logger.getLogger(FunDeepEqual.class);

    public final static FunctionSignature signatures[] = {
        new FunctionSignature(
            new QName("deep-equal", AtomBeatUtilModule.NAMESPACE_URI,
            		AtomBeatUtilModule.PREFIX),
            "Returns true() iff every item in $items-1 is deep-equal to the item " +
            "at the same position in $items-2, false() otherwise. " +
            "If both $items-1 and $items-2 are the empty sequence, returns true(). ",
            new SequenceType[] {
                new FunctionParameterSequenceType("items-1", Type.ITEM,
                    Cardinality.ZERO_OR_MORE, "The first item sequence"), 
                new FunctionParameterSequenceType("items-2", Type.ITEM,
                    Cardinality.ZERO_OR_MORE, "The second item sequence")
            },
            new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.ONE,
                "true() if the sequences are deep-equal, false() otherwise")
            ),
        new FunctionSignature(
            new QName("deep-equal", AtomBeatUtilModule.NAMESPACE_URI,
            		AtomBeatUtilModule.PREFIX),
            "Returns true() iff every item in $items-1 is deep-equal to the item " +
            "at the same position in $items-2, false() otherwise. " +
            "If both $items-1 and $items-2 are the empty sequence, returns true(). " +
            "Comparison collation is specified by $collation-uri. " + 
            THIRD_REL_COLLATION_ARG_EXAMPLE,
            new SequenceType[] {
                new FunctionParameterSequenceType("items-1", Type.ITEM,
                    Cardinality.ZERO_OR_MORE, "The first item sequence"), 
                new FunctionParameterSequenceType("items-2", Type.ITEM,
                    Cardinality.ZERO_OR_MORE, "The second item sequence"),
                new FunctionParameterSequenceType("collation-uri", Type.STRING,
                    Cardinality.EXACTLY_ONE, "The collation URI")
            },
            new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.ONE,
                "true() if the sequences are deep-equal, false() otherwise")
        ),
        new FunctionSignature(
                new QName("deep-equal", AtomBeatUtilModule.NAMESPACE_URI,
                		AtomBeatUtilModule.PREFIX),
                "Returns true() iff every item in $items-1 is deep-equal to the item " +
                "at the same position in $items-2, false() otherwise. " +
                "If both $items-1 and $items-2 are the empty sequence, returns true(). " +
                "Comparison collation is specified by $collation-uri. " + 
                THIRD_REL_COLLATION_ARG_EXAMPLE,
                new SequenceType[] {
                    new FunctionParameterSequenceType("items-1", Type.ITEM,
                        Cardinality.ZERO_OR_MORE, "The first item sequence"), 
                    new FunctionParameterSequenceType("items-2", Type.ITEM,
                        Cardinality.ZERO_OR_MORE, "The second item sequence"),
                    new FunctionParameterSequenceType("collation-uri", Type.STRING,
                        Cardinality.EXACTLY_ONE, "The collation URI"),
                    new FunctionParameterSequenceType("options", Type.STRING,
                                Cardinality.EXACTLY_ONE, "options")
                },
                new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.ONE,
                    "true() if the sequences are deep-equal, false() otherwise")
            )
    };

    public FunDeepEqual(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    public int getDependencies() {
        return Dependency.CONTEXT_SET | Dependency.CONTEXT_ITEM;
    }

    public Sequence eval(Sequence contextSequence, Item contextItem)
            throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);
            context.getProfiler().message(this, Profiler.DEPENDENCIES,
                "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES,
                    "CONTEXT SEQUENCE", contextSequence);
            if (contextItem != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES,
                    "CONTEXT ITEM", contextItem.toSequence());
        }
        Sequence result;
        Sequence[] args = getArguments(contextSequence, contextItem);
        Collator collator = getCollator(contextSequence, contextItem, 3);
        int length = args[0].getItemCount();
        if (length != args[1].getItemCount()) {
            result = BooleanValue.FALSE;
        } else {
            result = BooleanValue.TRUE;
            
            int flags = 0;
            if (args.length > 2) {
            	String options = args[3].getStringValue();
            	if (options.indexOf('w') >= 0) {
            		flags |= EXCLUDE_WHITESPACE_TEXT_NODES;
            	}
            }
            for (int i = 0; i < length; i++) {
                if (!deepEquals(args[0].itemAt(i), args[1].itemAt(i), collator, flags)) {
                    result = BooleanValue.FALSE;
                    break;
                }
            }
        }
        if (context.getProfiler().isEnabled()) 
            context.getProfiler().end(this, "", result); 
        return result;
    }

    public static boolean deepEquals(Item a, Item b, Collator collator, int flags) {
        try {
            final boolean aAtomic = Type.subTypeOf(a.getType(), Type.ATOMIC);
            final boolean bAtomic = Type.subTypeOf(b.getType(), Type.ATOMIC);
            if (aAtomic || bAtomic) {
                if (!aAtomic || !bAtomic)
                    return false;
                try {
                    AtomicValue av = (AtomicValue) a;
                    AtomicValue bv = (AtomicValue) b;
                    if (Type.subTypeOf(av.getType(), Type.NUMBER) &&
                        Type.subTypeOf(bv.getType(), Type.NUMBER)) {
                        //or if both values are NaN
                        if (((NumericValue) a).isNaN() && ((NumericValue) b).isNaN())
                            return true;
                    }
                    return ValueComparison.compareAtomic(collator, av, bv,
                        Constants.TRUNC_NONE, Constants.EQ);
                } catch (XPathException e) {
                    return false;
                }
            }
            if (a.getType() != b.getType())
                return false;
            NodeValue nva = (NodeValue) a, nvb = (NodeValue) b;
            if (nva == nvb) return true;
            try {
                //Don't use this shortcut for in-memory nodes
                //since the symbol table is ignored.
                if (nva.getImplementationType() != NodeValue.IN_MEMORY_NODE &&
                    nva.equals(nvb))
                    return true; // shortcut!
            } catch (XPathException e) {
                // apparently incompatible values, do manual comparison
            }
            Node na, nb;
            switch(a.getType()) {
            case Type.DOCUMENT:
                // NodeValue.getNode() doesn't seem to work for document nodes
                na = nva instanceof Node ? (Node) nva : ((NodeProxy) nva).getDocument();
                nb = nvb instanceof Node ? (Node) nvb : ((NodeProxy) nvb).getDocument();
                return compareContents(na, nb, flags);
            case Type.ELEMENT:
                na = nva.getNode();
                nb = nvb.getNode();
                return compareElements(na, nb, flags);
            case Type.ATTRIBUTE:
                na = nva.getNode();
                nb = nvb.getNode();
                return compareNames(na, nb)
                    && safeEquals(na.getNodeValue(), nb.getNodeValue());
            case Type.PROCESSING_INSTRUCTION:
            case Type.NAMESPACE:
                na = nva.getNode(); nb = nvb.getNode();
                return safeEquals(na.getNodeName(), nb.getNodeName()) &&
                    safeEquals(nva.getStringValue(), nvb.getStringValue());
            case Type.TEXT:
            case Type.COMMENT:
                return safeEquals(nva.getStringValue(), nvb.getStringValue());
            default:
                throw new RuntimeException("unexpected item type " + Type.getTypeName(a.getType()));
            }
        } catch (XPathException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean compareElements(Node a, Node b, int flags) {
        return compareNames(a, b) && compareAttributes(a, b) &&
            compareContents(a, b, flags);
    }

    private static boolean compareContents(Node a, Node b, int flags) {
        a = findNextTextOrElementNode(a.getFirstChild(), flags);
        b = findNextTextOrElementNode(b.getFirstChild(), flags);
        while (!(a == null || b == null)) {
            int nodeTypeA = getEffectiveNodeType(a);
            int nodeTypeB = getEffectiveNodeType(b);
            if (nodeTypeA != nodeTypeB)
                return false;
            switch (nodeTypeA) {
            case Node.TEXT_NODE:
                if (a.getNodeType() == NodeImpl.REFERENCE_NODE &&
                        b.getNodeType() == NodeImpl.REFERENCE_NODE) {
                    if (!safeEquals(((ReferenceNode)a).getReference().getNodeValue(),
                            ((ReferenceNode)b).getReference().getNodeValue()))
                        return false;
                } else if (a.getNodeType() == NodeImpl.REFERENCE_NODE) {
                    if (!safeEquals(((ReferenceNode)a).getReference().getNodeValue(),
                            b.getNodeValue()))
                        return false;
                } else if (b.getNodeType() == NodeImpl.REFERENCE_NODE) {
                    if (!safeEquals(a.getNodeValue(), 
                            ((ReferenceNode)b).getReference().getNodeValue()))
                        return false;
                } else {
                    if (!safeEquals(a.getNodeValue(), b.getNodeValue()))
                        return false;
                }
                break;
            case Node.ELEMENT_NODE:
                if (!compareElements(a, b, flags))
                    return false;
                break;
            default:
                throw new RuntimeException("unexpected node type " + nodeTypeA);
            }
            a = findNextTextOrElementNode(a.getNextSibling(), flags);
            b = findNextTextOrElementNode(b.getNextSibling(), flags);
        }
        return a == b; // both null
    }

    private static boolean[] C0WHITE = {
        false, false, false, false, false, false, false, false,  // 0-7
        false, true, true, false, false, true, false, false,     // 8-15
        false, false, false, false, false, false, false, false,  // 16-23
        false, false, false, false, false, false, false, false,  // 24-31
        true                                                     // 32
    };
    
    /**
     * Determine if a string is all-whitespace
     *
     * @param content the string to be tested
     * @return true if the supplied string contains no non-whitespace
     *     characters
     */

    public static boolean isWhite(CharSequence content) {
        final int len = content.length();
        for (int i=0; i<len;) {
            // all valid XML 1.0 whitespace characters, and only whitespace characters, are <= 0x20
            // But XML 1.1 allows non-white characters that are also < 0x20, so we need a specific test for these
            char c = content.charAt(i++);
            if (c > 32 || !C0WHITE[c]) {
                return false;
            }
        }
        return true;
    }
 
    private static boolean isIgnorable(Node node, int flags) {
		final int kind = node.getNodeType();
		if (kind == Type.COMMENT) {
			return (flags & INCLUDE_COMMENTS) == 0;
		} else if (kind == Type.PROCESSING_INSTRUCTION) {
			return (flags & INCLUDE_PROCESSING_INSTRUCTIONS) == 0;
		} else if (kind == Type.TEXT) {
			return ((flags & EXCLUDE_WHITESPACE_TEXT_NODES) != 0)
					&& Whitespace.isWhite(node.getNodeValue());
		}
		return false;
	}
    
    private static Node findNextTextOrElementNode(Node n, int flags) {
        for(;;) {
            if (n == null)
                return null;
            int nodeType = getEffectiveNodeType(n);
            if (nodeType == Node.ELEMENT_NODE) {
                return n;
            }
            if(nodeType == Node.TEXT_NODE) {
            	if (!isIgnorable(n,flags)) {
            		return n;
            	}
            }
            n = n.getNextSibling();
        }
    }

    private static int getEffectiveNodeType(Node n) {
        int nodeType = n.getNodeType();
        if (nodeType == NodeImpl.REFERENCE_NODE) {
            nodeType = ((ReferenceNode) n).getReference().getNode().getNodeType();
        }
        return nodeType;
    }

    private static boolean compareAttributes(Node a, Node b) {
        NamedNodeMap nnma = a.getAttributes();
        NamedNodeMap nnmb = b.getAttributes();
        if (getAttrCount(nnma) != getAttrCount(nnmb)) return false;
        for (int i = 0; i < nnma.getLength(); i++) {
            Node ta = nnma.item(i);
            if (Namespaces.XMLNS_NS.equals(ta.getNamespaceURI()))
                continue;
            Node tb = ta.getLocalName() == null ?
                nnmb.getNamedItem(ta.getNodeName()) :
                nnmb.getNamedItemNS(ta.getNamespaceURI(), ta.getLocalName());
            if (tb == null || !safeEquals(ta.getNodeValue(), tb.getNodeValue()))
                return false;
        }
        return true;
    }

    /**
     * Return the number of real attributes in the map. Filter out
     * xmlns namespace attributes.
     *
     * @param nnm
     * @return
     */
    private static int getAttrCount(NamedNodeMap nnm) {
        int count = 0;
        for (int i=0; i<nnm.getLength(); i++) {
            Node n = nnm.item(i);
            if (!Namespaces.XMLNS_NS.equals(n.getNamespaceURI()))
                ++count;
        }
        return count;
    }

    private static boolean compareNames(Node a, Node b) {
        if (a.getLocalName() != null || b.getLocalName() != null) {
            return safeEquals(a.getNamespaceURI(), b.getNamespaceURI()) &&
                safeEquals(a.getLocalName(), b.getLocalName());
        }
        return safeEquals(a.getNodeName(), b.getNodeName());
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

}
