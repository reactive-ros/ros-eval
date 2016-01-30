package ros_eval;

import org.reactive_ros.Stream;
import org.reactive_ros.internal.graph.FlowGraph;
import org.reactive_ros.internal.output.Output;
import org.apache.commons.lang.StringUtils;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.reactive_ros.evaluation.EvaluationStrategy;
import org.apache.commons.logging.Log;

/**
 * A ROS node that executes a given {@link FlowGraph} and redirects the resulting stream to given {@link Output}.
 * @author Orestis Melkonian
 */
public class ReactiveNodeMain extends AbstractNodeMain {
    private String graphName;
    private Log log;
    private EvaluationStrategy evaluationStrategy;

    private Stream stream;
    private Output output;

    /**
     * @param graphName the graph name of this {@link ReactiveNodeMain}
     * @param stream the {@link Stream} to be evaluated by this {@link ReactiveNodeMain}
     * @param output the {@link Output} to redirect the evaluated stream
     * @param evaluationStrategy the {@link EvaluationStrategy} to use
     * @param log the {@link Log} to output messages
     */
    public ReactiveNodeMain(String graphName, Stream stream, Output output, EvaluationStrategy evaluationStrategy, Log log) {
        this.graphName = graphName;
        this.log = log;
        this.evaluationStrategy = evaluationStrategy;
        this.stream = stream;
        this.output = output;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(graphName);
    }

    /**
     * Evaluates this {@link ReactiveNodeMain}'s {@link FlowGraph}.
     * @param connectedNode automatically passed from rosjava
     */
    @Override
    public void onStart(ConnectedNode connectedNode) {
        if (log == null)
            log = connectedNode.getLog();

        /**
         * Display
         */
        /*String label = graphName + " ["+ processInfo() + "]";
        log.info("\n\n======================== " + label + " ========================"
                + "\n" + stream.getGraph()
                + "\n\t===>\t" + output + "\n"
                + "\n=========================" + StringUtils.repeat("=", label.length()) + "=========================\"\n\n");*/
        /**
         * Execute
         */
        // TODO Add default backpressure() or cache()
        evaluationStrategy.evaluate(stream, output);
    }

    private static String processInfo() {
//        return ManagementFactory.getRuntimeMXBean().getName() + "@" + processInfo() + ": ";
        return Thread.currentThread().getName();
    }
}
