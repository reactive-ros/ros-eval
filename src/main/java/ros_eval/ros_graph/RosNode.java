package ros_eval.ros_graph;

import org.rhea_core.internal.expressions.Transformer;

/**
 * @author Orestis Melkonian
 */
public class RosNode {
    private Transformer transformer;

    public RosNode(Transformer transformer) {
        this.transformer = transformer;
    }

    public Transformer getTransformer() {
        return transformer;
    }

    @Override
    public int hashCode() {
        return transformer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return !(obj == null || !(obj instanceof RosNode)) && transformer.equals(((RosNode) obj).transformer);
    }

    @Override
    public String toString() {
        return transformer.toString();
    }
}
