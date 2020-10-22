package org.radar.radarlint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.radar.radarlint.settings.ProjectKeySeparatorPreference;

/**
 *
 * @author Víctor
 */
public class MvnProjectAnalyzer {

    public FileObject getPomFileObject (Project project) {
        return project.getProjectDirectory().getFileObject("pom.xml");
    }

    public Model createModel (Project project) {
        FileObject pomFile = getPomFileObject(project);
        if (pomFile == null) {
            return null;
        }
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try (final Reader reader = new InputStreamReader(pomFile.getInputStream())) {
            Model model = mavenreader.read(reader);
            model.setPomFile(new File(pomFile.getPath()));
            return model;
        } catch (XmlPullParserException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getProjectKey (Model model) {
        //If the maven model has a sonar project key, use it.
        //If not, check if the project has an ancestor in the same multi-module maven project that does.
        //Lastly, if the parent (recursive) doesn't have a project key, just generate a default.
        String projectKey = extractProjectKey(model);
        if (projectKey != null) {
            return projectKey;
        }
        projectKey = getProjectKeyFromAncestor(model, true);
        if (projectKey != null) {
            return projectKey;
        }
        return createDefaultProjectKey(model);
    }

    private String createDefaultProjectKey (Model model) {
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        return buildProjectKey(groupId, model.getArtifactId(), new ProjectKeySeparatorPreference().getValue());
    }

    private String buildProjectKey (String groupId, String artifactId, String separator) {
        return groupId + separator + artifactId;
    }

    private String extractProjectKey (Model model) {
        //TODO: Don't just use the model, get the effective project model, with properties evaluated.
        //Our projects are multi-module maven projects where the top-level parent has a property present, and sometimes
        //it's actually depending on defined properties passed in via command line (or inheritence).
        return model.getProperties().getProperty("sonar.projectKey");
    }

    private String getProjectKeyFromAncestor (Model child, boolean loadPoms) {
        if (child.getParent() == null) {
            return null;
        }

        if (loadPoms) {
            Model parentModel = getParentModel(child);
            if (parentModel != null) {
                String projectKey = extractProjectKey(parentModel);
                if (projectKey != null) {
                    return projectKey;
                }
                return getProjectKeyFromAncestor(parentModel, loadPoms);
            }
        } else {
            if (child.getParent() != null) {
                String groupId = child.getParent().getGroupId();
                String artifactId = child.getParent().getArtifactId();
                return buildProjectKey(groupId, artifactId, new ProjectKeySeparatorPreference().getValue());
            }
        }

        return null;
    }

    private Model getParentModel (Model child) {
        Parent parent = child.getParent();
        if (parent != null) {
            String parentRelativePomPath = parent.getRelativePath();

            if (parentRelativePomPath != null) {
                Path childPomPath = Paths.get(child.getPomFile().getPath());
                Path childPomDir = childPomPath.getParent();

                Path parentPomPath = childPomDir.resolve(parentRelativePomPath);
                Model parentModel = readModel(parentPomPath);
                return parentModel;
            }
        }

        return null;
    }

    private Model readModel (Path pomPath) {
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        File pomFile = pomPath.toFile();
        try (
            final InputStream is = Files.newInputStream(pomPath);
            final Reader reader = new InputStreamReader(is)) {
            Model model = mavenreader.read(reader);
            model.setPomFile(new File(pomFile.getPath()));
            return model;
        } catch (XmlPullParserException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
