package ros_eval;

import org.reactive_ros.Stream;
import org.reactive_ros.evaluation.EvaluationStrategy;
import org.reactive_ros.internal.output.*;
import org.ros.RosCore;
import org.ros.node.*;

/**
 * Evaluates a dataflow graph by mapping one or more stream operators to individual ROS nodes
 * to run concurrently, using the ROS message system to communicate.
 * @author Orestis Melkonian
 */
public class RosEvaluationStrategy implements EvaluationStrategy {
    private EvaluationStrategy evaluationStrategy;
    private final NodeMainExecutor executor = DefaultNodeMainExecutor.newDefault();
    private NodeConfiguration config;
    public RosRunner runner;

    public RosEvaluationStrategy(EvaluationStrategy evaluationStrategy) {
        RosCore roscore = RosCore.newPublic();
        roscore.start();
        try {
            roscore.awaitStart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        config = NodeConfiguration.newPrivate(roscore.getUri());
        runner = new RosRunner(executor, config, evaluationStrategy);
        this.evaluationStrategy = evaluationStrategy;
    }

    public RosEvaluationStrategy(EvaluationStrategy evaluationStrategy, String nodePrefix) {
        this(evaluationStrategy);
        runner.nodePrefix = nodePrefix;
    }

    @Override
    public <T> void evaluate(Stream<T> stream, Output output) {
        runner.set(stream, output);
        executor.execute(runner, config);
    }

    @Override
    public EvaluationStrategy newStrategy() {
        return new RosEvaluationStrategy(evaluationStrategy);
    }

}
