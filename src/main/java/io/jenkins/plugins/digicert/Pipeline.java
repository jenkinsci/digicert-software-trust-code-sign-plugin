//The MIT License
//
//Copyright 2023
//
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package io.jenkins.plugins.digicert;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class Pipeline extends Step {

    @DataBoundConstructor
    public Pipeline() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(context, this);
    }

    private static class ExecutionImpl extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private final transient Pipeline step;
        private EnvVars envVars = null;

        ExecutionImpl(StepContext context, Pipeline step) throws Exception {
            super(context);
            this.step = step;
            this.envVars = context.get(EnvVars.class);
        }

        @Override
        public Void run() throws IOException, InterruptedException, ExecutionException {
            TaskListener listener = getContext().get(TaskListener.class);
            FilePath filePath = getContext().get(FilePath.class);
            assert filePath != null;
            VirtualChannel virtualChannel = filePath.getChannel();
            assert virtualChannel != null;

            virtualChannel.callAsync(new AgentInfo(listener, envVars)).get();
            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

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