package net.liftweb;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import java.io.File;

import net.liftweb.json.xschema.codegen.BaseScalaCodeGenerator;

/**
 * Goal which runs the XSchema code generation 
 *
 * @goal generate-sources
 * @phase generate-sources
 */
public class XSchemaMojo extends AbstractMojo {
    /**
     * Location of the xschema files.
     *
     * @parameter expression="${sourceDirectory}" default-value="${basedir}/src/main/xschema"
     */
    private File sourceDirectory;

    /**
     * Location of the generated sources.
     *
     * @parameter expression="${outputDirectory}" default-value="${project.build.directory}/generated-sources/xschema"
     */
    private File outputDirectory;

    /**
     * Location of the generated sources.
     *
     * @parameter expression="${testOutputDirectory}" default-value="${project.build.directory}/generated-test-sources/xschema"
     */
    private File testOutputDirectory;

    /**
    * A set of Ant-like inclusion patterns used to select files from
    * the source directory for processing. By default, the pattern
    * <code>**&#47;*.json</code> is used to select grammar files.
    *
    * @parameter
    */
    private String[] includes = new String[] { "**/*.json" };

    /**
    * A set of Ant-like exclusion patterns used to prevent certain
    * files from being processed. By default, this set is empty such
    * that no files are excluded.
    *
    * @parameter
    */
    private String[] excludes = new String[0];

    /**
     * A set of namespaces (packages) for which classes will be generated.
     * By default, this set is empty, which will cause all classes defined
     * in the XSchema file to be generated; if this set is nonempty then
     * the restriction is applied.
     * 
     * @parameter
     */
    private String[] namespaces = new String[0];

    /**
    * The current Maven project.
    *
    * @parameter default-value="${project}"
    * @readonly
    * @required
    */
    private MavenProject project;

    private FileSetManager fileSetManager = new FileSetManager();

    public XSchemaMojo() { }

    public void execute() throws MojoExecutionException {
        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException(sourceDirectory + "is not a directory");
        }

        FileSet fs = new FileSet();
        fs.setDirectory(sourceDirectory.getAbsolutePath());
        fs.setFollowSymlinks( false );

        for (String include : includes) fs.addInclude(include);
        for (String exclude : excludes) fs.addExclude(exclude);

        String[] includedFiles = fileSetManager.getIncludedFiles(fs);
        for(int i = 0; i < includedFiles.length; i++) {
          includedFiles[i] = new File(sourceDirectory, includedFiles[i]).getAbsolutePath();
        }

        getLog().info("Generating code from " + java.util.Arrays.deepToString(includedFiles));

        BaseScalaCodeGenerator generator = new BaseScalaCodeGenerator();
        generator.generateFromFiles(
            includedFiles, 
            outputDirectory.getAbsolutePath(), 
            testOutputDirectory.getAbsolutePath(),
            namespaces
        );

        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        project.addTestCompileSourceRoot(testOutputDirectory.getAbsolutePath());
    }
}
