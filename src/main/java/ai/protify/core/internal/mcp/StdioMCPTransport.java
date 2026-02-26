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

package ai.protify.core.internal.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class StdioMCPTransport implements MCPTransport {

    private final String[] command;
    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;

    public StdioMCPTransport(String[] command) {
        this.command = command;
    }

    @Override
    public void open() {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            this.process = pb.start();
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start MCP server process", e);
        }
    }

    @Override
    public String sendRequest(String jsonRpc) {
        try {
            writer.write(jsonRpc);
            writer.newLine();
            writer.flush();

            // Read response line
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalStateException("MCP server closed connection unexpectedly");
            }
            return line;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to communicate with MCP server", e);
        }
    }

    @Override
    public boolean isOpen() {
        return process != null && process.isAlive();
    }

    @Override
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (process != null) {
                process.destroyForcibly();
                process.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
