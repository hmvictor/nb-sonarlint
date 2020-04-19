package org.radar.radarlint;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Víctor
 */
public class MvnProjectAnalyzer {
    
    public FileObject getPomFileObject(Project project) {
        return project.getProjectDirectory().getFileObject("pom.xml");
    }

    public Model createModel(Project project) {
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

    public String getProjectKey(Model model) {
        String projectKey = model.getProperties().getProperty("sonar.projectKey");
        if (projectKey != null) {
            return projectKey;
        }
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        return groupId + ":" + model.getArtifactId();
    }
    
}
