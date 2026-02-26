/*
 * Copyright(c) 2026 Protify Consulting LLC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ai.protify.core.request;

import ai.protify.core.internal.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

public final class AIFileInput implements AIInput {

    private final FileDataReferenceType referenceType;
    private final InputType type;
    private final String filename;
    private final String data;

    private AIFileInput(InputType type, FileDataReferenceType referenceType, String filename, String data) {
        this.type = type;
        this.referenceType = referenceType;
        this.filename = filename;
        this.data = data;
    }

    public static AIFileInput fromDataUrl(String theFilename, String base64EncodedUrl) {
        if (base64EncodedUrl == null || !base64EncodedUrl.startsWith("data:")) {
            throw new IllegalArgumentException("Invalid Base64 Data URL. It must start with 'data:'");
        }
        InputType fileType = InputType.IMAGE;
        String extension = FileUtil.extractExtensionFromDataUrl(base64EncodedUrl);
        if ("pdf".equals(extension)) {
            fileType = InputType.PDF;
        }
        if (theFilename == null) {
            theFilename = UUID.randomUUID() + "." + extension;
        }
        return new AIFileInput(fileType, FileDataReferenceType.DATA_URL, theFilename, base64EncodedUrl);
    }

    public static AIFileInput fromDataUrl(String base64EncodedUrl) {
        return fromDataUrl(null, base64EncodedUrl);
    }

    public static AIFileInput fromFile(File file) {
        try {
            String fileName = file.getName();
            InputType fileType = determineFileType(fileName);
            String mimeType = Files.probeContentType(file.toPath());
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileContent);
            return new AIFileInput(fileType, FileDataReferenceType.DATA_URL, fileName, dataUrl);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + file.getPath(), e);
        }
    }

    public static AIFileInput fromFilePath(String filePath) {
        return fromFile(new File(filePath));
    }

    public static AIFileInput fromProviderId(InputType fileType, String providerId) {
        return new AIFileInput(fileType, FileDataReferenceType.DATA_URL, null, providerId);
    }

    public static AIFileInput fromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        InputType fileType = determineFileType(fileName);
        return new AIFileInput(fileType, FileDataReferenceType.HTTP_URL, fileName, url);
    }

    public static AIFileInput fromClasspath(String path) {
        File fileFromClasspath;
        URL resource = AIFileInput.class.getResource(path);
        if (resource != null) {
            try {
                fileFromClasspath = new File(resource.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid classpath path format: " + path, e);
            }
            return fromFile(fileFromClasspath);
        }
        throw new IllegalArgumentException("Classpath resource not found: " + path);
    }


    public FileDataReferenceType getReferenceType() {
        return referenceType;
    }

    @Override
    public InputType getType() {
        return type;
    }

    public String getFilename() {
        return filename;
    }

    public String getData() {
        return data;
    }

    private static InputType determineFileType(String fileName) {
        String extension = FileUtil.getFileExtension(fileName);
        if ("pdf".equals(extension)) {
            return InputType.PDF;
        }
        if ("png".equals(extension) || "jpg".equals(extension) ||
                "jpeg".equals(extension) || "webp".equals(extension) ||
                "gif".equals(extension) || "heic".equals(extension) ||
                "heif".equals(extension)) {
            return InputType.IMAGE;
        }
        throw new IllegalArgumentException("Unsupported file type for file: " + fileName);
    }
}
