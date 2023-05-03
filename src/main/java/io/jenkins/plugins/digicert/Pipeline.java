package io.jenkins.plugins.digicert;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import hudson.EnvVars;

import com.google.common.collect.ImmutableSet;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class Pipeline extends Step {

    @DataBoundConstructor
    public Pipeline() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(context, this);
    }

    private static class ExecutionImpl extends  SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private final transient Pipeline step;
        private EnvVars envVars = null;

        ExecutionImpl(StepContext context, Pipeline step) throws Exception {
            super(context);
            this.step = step;
            this.envVars = context.get(EnvVars.class);
        }

        @Override public Void run() throws IOException, InterruptedException, ExecutionException {
            TaskListener listener = getContext().get(TaskListener.class);
            FilePath filePath = getContext().get(FilePath.class);
            assert filePath != null;
            VirtualChannel virtualChannel = filePath.getChannel();
            assert virtualChannel != null;

            virtualChannel.callAsync(new AgentInfo(listener , envVars)).get();
            return null;
        }
    }

    @Extension public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
                return "SoftwareTrustManagerSetup";
        }

        @Override
        public String getFunctionName() {
            return "SoftwareTrustManagerSetup";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }
    }
}