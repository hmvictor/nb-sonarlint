package org.radar.radarlint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.openide.filesystems.FileObject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

/**
 *
 * @author Víctor
 */
public class ContentClientInputFile implements ClientInputFile{
    private FileObject fileObject;
    private String content;
    private boolean isTestFile;

    public ContentClientInputFile(FileObject fileObject, String content, boolean isTestFile) {
        this.fileObject = fileObject;
        this.isTestFile=isTestFile;
        this.content=content;
    }
    
    @Override
    public String getPath() {
        return fileObject.getPath();
    }

    @Override
    public boolean isTest() {
        return isTestFile;
    }

    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public FileObject getClientObject() {
        return fileObject;
    }

    @Override
    public InputStream inputStream() throws IOException {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String contents() throws IOException {
        return content;
    }

    @Override
    public String relativePath() {
        return fileObject.getNameExt();
    }

    @Override
    public URI uri() {
        return fileObject.toURI();
    }
    
}
