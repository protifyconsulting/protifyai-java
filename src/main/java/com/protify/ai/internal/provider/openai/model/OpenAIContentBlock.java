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

package com.protify.ai.internal.provider.openai.model;

import com.protify.ai.internal.util.json.ProtifyJsonProperty;

public final class OpenAIContentBlock {

    private String type;
    private String text;

    @ProtifyJsonProperty("image_url")
    private String imageUrl;

    @ProtifyJsonProperty("file_id")
    private String fileId;

    @ProtifyJsonProperty("file_url")
    private String fileUrl;

    private String filename;

    private OpenAIContentBlock() {
    }

    public static OpenAIContentBlock text(String text) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "input_text";
        block.text = text;
        return block;
    }

    public static OpenAIContentBlock outputText(String text) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "output_text";
        block.text = text;
        return block;
    }

    public static OpenAIContentBlock imageFromUrl(String imageUrl) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "input_image";
        block.imageUrl = imageUrl;
        return block;
    }

    public static OpenAIContentBlock imageFromFileId(String fileId) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "input_image";
        block.fileId = fileId;
        return block;
    }

    public static OpenAIContentBlock fileFromId(String fileId) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "input_file";
        block.fileId = fileId;
        return block;
    }

    public static OpenAIContentBlock fileFromUrl(String fileUrl) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "input_file";
        block.fileUrl = fileUrl;
        return block;
    }

    public static OpenAIContentBlock fileFromUrl(String fileUrl, String filename) {
        OpenAIContentBlock block = new OpenAIContentBlock();
        block.type = "input_file";
        block.fileUrl = fileUrl;
        block.filename = filename;
        return block;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    @ProtifyJsonProperty("image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    @ProtifyJsonProperty("file_id")
    public String getFileId() {
        return fileId;
    }

    @ProtifyJsonProperty("file_url")
    public String getFileUrl() {
        return fileUrl;
    }

    public String getFilename() {
        return filename;
    }
}
