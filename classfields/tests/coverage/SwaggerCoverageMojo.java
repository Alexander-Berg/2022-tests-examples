package ru.auto.tests.coverage;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Paths;

import static ru.auto.tests.coverage.SwaggerCoverageExec.swaggerCoverage;

@Mojo(name = "run", defaultPhase = LifecyclePhase.TEST)
public class SwaggerCoverageMojo extends AbstractMojo {

    @Parameter(name = "inputSpec", property = "swagger.coverage.inputSpec", required = true)
    private String inputSpec;

    @Parameter(name = "inputReq", property = "swagger.coverage.inputReq", required = true)
    private String inputReq;

    @Parameter(name = "output", property = "swagger.coverage.output", defaultValue = "${project.build.directory}")
    private String output;

    @Parameter(name = "skip", property = "swagger.coverage.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (skip) {
                getLog().info("Swagger coverage is skipped.");
                return;
            }
            Config config = Config.conf().withReqPath(Paths.get(inputReq)).withSpecPath(Paths.get(inputSpec))
                    .withOutputPath(Paths.get(output));
            swaggerCoverage(config).execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Exception while run coverage.", e);
        }
    }
}

