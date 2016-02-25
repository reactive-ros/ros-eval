package ros_eval.ros_graph;

import org.jgrapht.graph.DefaultEdge;
import ros_eval.RosTopic;

/**
 * @author Orestis Melkonian
 */
public class RosEdge extends DefaultEdge {
    private RosNode source;
    private RosNode target;
    private RosTopic topic;

    public RosEdge(RosNode v1, RosNode v2, RosTopic topic) {
        this.source = v1;
        this.target = v2;
        this.topic = topic;
    }

    public RosTopic getTopic() {
        return topic;
    }

    @Override
    public RosNode getSource() {
        return source;
    }

    @Override
    public RosNode getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof RosEdge)
                && ((RosEdge) obj).getSource().equals(source)
                && ((RosEdge) obj).getTarget().equals(target);
    }

    @Override
    public String toString() {
        return topic.getName();
    }
}
