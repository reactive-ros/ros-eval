package ros_eval.ros_graph;

import org.reactive_ros.internal.expressions.NoInputExpr;
import org.reactive_ros.internal.expressions.Transformer;
import org.reactive_ros.internal.graph.FlowGraph;
import org.reactive_ros.internal.graph.SimpleEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.reactive_ros.util.functions.Func0;
import ros_eval.RosTopic;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Orestis Melkonian
 */
public class RosGraph extends DirectedPseudograph<RosNode, RosEdge> {
    public Transformer toConnect;

    public RosGraph(FlowGraph graph, Func0<RosTopic> topicGenerator) {
        super(RosEdge.class);
        // Copy FlowGraph
        toConnect = graph.getConnectNode();
        Map<Transformer, RosNode> mapper = new HashMap<>();
        for (Transformer p : graph.vertexSet()) {
            RosNode toAdd;
            if (p == graph.getConnectNode()) {
                Transformer connectNode = p.clone();
                toConnect = connectNode;
                toAdd = new RosNode(connectNode);
            }
            else
                toAdd = new RosNode(p.clone());
            mapper.put(p, toAdd);
            addVertex(toAdd);
        }
        for (SimpleEdge e : graph.edgeSet()) {
            RosNode source = mapper.get(e.getSource());
            RosNode target = mapper.get(e.getTarget());
            addEdge(source, target, new RosEdge(source, target, topicGenerator.call()));
        }
    }

    public List<RosNode> predecessors(RosNode target) {
        return edgeSet().stream().filter(e -> e.getTarget().equals(target)).map(RosEdge::getSource).collect(Collectors.toList());
    }

    public List<RosNode> getRoots() {
        return vertexSet().stream().filter(p -> p.getTransformer() instanceof NoInputExpr).collect(Collectors.toList());
    }
}
