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

package com.protify.ai.response;

public enum MimeType {
    JSON("application/json"),
    TEXT("text/plain"),
    HTML("text/html"),
    XML("application/xml"),
    CSV("text/csv"),
    MARKDOWN("text/markdown"),
    YAML("text/yaml"),

    PDF("application/pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    JPG("image/jpeg"),
    JPEG("image/jpeg"),
    PNG("image/png"),
    GIF("image/gif"),
    WEBP("image/webp"),
    HEIC("image/heic"),
    HEIF("image/heif"),
    AVIF("image/avif"),
    SVG("image/svg+xml"),
    TIFF("image/tiff"),

    MP3("audio/mpeg"),
    WAV("audio/wav"),
    M4A("audio/mp4"),
    FLAC("audio/x-flac"),

    MP4("video/mp4"),
    MOV("video/quicktime"),
    AVI("video/x-msvideo"),

    OCTET_STREAM("application/octet-stream");

    private final String name;

    MimeType(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return name;
    }
}
