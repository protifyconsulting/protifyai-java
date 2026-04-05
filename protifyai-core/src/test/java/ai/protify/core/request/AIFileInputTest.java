package ai.protify.core.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static ai.protify.core.request.InputType.*;

class AIFileInputTest {

    // ---------------------------------------------------------------
    // 1. fromDataUrl
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromDataUrl")
    class FromDataUrl {

        @Test
        @DisplayName("Parses valid PNG data URL")
        void validPngDataUrl() {
            String dataUrl = "data:image/png;base64,iVBORw0KGgo=";
            AIFileInput input = AIFileInput.fromDataUrl(dataUrl);

            assertEquals(InputType.IMAGE, input.getType());
            assertEquals(FileDataReferenceType.DATA_URL, input.getReferenceType());
            assertEquals(dataUrl, input.getData());
            assertNotNull(input.getFilename());
            assertTrue(input.getFilename().endsWith(".png"));
        }

        @Test
        @DisplayName("Parses valid PDF data URL")
        void validPdfDataUrl() {
            String dataUrl = "data:application/pdf;base64,JVBERi0=";
            AIFileInput input = AIFileInput.fromDataUrl(dataUrl);

            assertEquals(InputType.PDF, input.getType());
        }

        @Test
        @DisplayName("Accepts custom filename")
        void customFilename() {
            String dataUrl = "data:image/jpeg;base64,/9j/4AAQ=";
            AIFileInput input = AIFileInput.fromDataUrl("photo.jpg", dataUrl);

            assertEquals("photo.jpg", input.getFilename());
        }

        @Test
        @DisplayName("Throws on null data URL")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    AIFileInput.fromDataUrl(null)
            );
        }

        @Test
        @DisplayName("Throws on invalid data URL")
        void throwsOnInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                    AIFileInput.fromDataUrl("not-a-data-url")
            );
        }

        @Test
        @DisplayName("Throws on URL without data: prefix")
        void throwsWithoutDataPrefix() {
            assertThrows(IllegalArgumentException.class, () ->
                    AIFileInput.fromDataUrl("https://example.com/image.png")
            );
        }
    }

    // ---------------------------------------------------------------
    // 2. fromFile
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromFile")
    class FromFile {

        @Test
        @DisplayName("Reads PNG file")
        void readsPngFile(@TempDir Path tempDir) throws IOException {
            Path pngFile = tempDir.resolve("test.png");
            // Minimal PNG header
            Files.write(pngFile, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

            AIFileInput input = AIFileInput.fromFile(pngFile.toFile());

            assertEquals("test.png", input.getFilename());
            assertEquals(InputType.IMAGE, input.getType());
            assertEquals(FileDataReferenceType.DATA_URL, input.getReferenceType());
            assertTrue(input.getData().startsWith("data:"));
            assertTrue(input.getData().contains("base64,"));
        }

        @Test
        @DisplayName("Reads JPEG file")
        void readsJpegFile(@TempDir Path tempDir) throws IOException {
            Path jpgFile = tempDir.resolve("photo.jpg");
            Files.write(jpgFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            AIFileInput input = AIFileInput.fromFile(jpgFile.toFile());

            assertEquals("photo.jpg", input.getFilename());
            assertEquals(InputType.IMAGE, input.getType());
        }

        @Test
        @DisplayName("Reads PDF file")
        void readsPdfFile(@TempDir Path tempDir) throws IOException {
            Path pdfFile = tempDir.resolve("doc.pdf");
            Files.write(pdfFile, "%PDF-1.4".getBytes());

            AIFileInput input = AIFileInput.fromFile(pdfFile.toFile());

            assertEquals("doc.pdf", input.getFilename());
            assertEquals(InputType.PDF, input.getType());
        }

        @Test
        @DisplayName("Throws on nonexistent file")
        void throwsOnMissingFile() {
            File missing = new File("/nonexistent/path/image.png");
            assertThrows(Exception.class, () -> AIFileInput.fromFile(missing));
        }

        @Test
        @DisplayName("Throws on unsupported file type")
        void throwsOnUnsupportedType(@TempDir Path tempDir) throws IOException {
            Path txtFile = tempDir.resolve("notes.txt");
            Files.write(txtFile, "hello".getBytes());

            assertThrows(IllegalArgumentException.class, () ->
                    AIFileInput.fromFile(txtFile.toFile())
            );
        }
    }

    // ---------------------------------------------------------------
    // 3. fromFilePath
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromFilePath")
    class FromFilePath {

        @Test
        @DisplayName("Delegates to fromFile")
        void delegatesToFromFile(@TempDir Path tempDir) throws IOException {
            Path pngFile = tempDir.resolve("test.png");
            Files.write(pngFile, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

            AIFileInput input = AIFileInput.fromFilePath(pngFile.toString());

            assertEquals("test.png", input.getFilename());
            assertEquals(InputType.IMAGE, input.getType());
        }
    }

    // ---------------------------------------------------------------
    // 4. fromUrl
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromUrl")
    class FromUrl {

        @Test
        @DisplayName("Parses image URL")
        void parsesImageUrl() {
            AIFileInput input = AIFileInput.fromUrl("https://example.com/photos/cat.png");

            assertEquals("cat.png", input.getFilename());
            assertEquals(InputType.IMAGE, input.getType());
            assertEquals(FileDataReferenceType.HTTP_URL, input.getReferenceType());
            assertEquals("https://example.com/photos/cat.png", input.getData());
        }

        @Test
        @DisplayName("Parses PDF URL")
        void parsesPdfUrl() {
            AIFileInput input = AIFileInput.fromUrl("https://example.com/report.pdf");

            assertEquals("report.pdf", input.getFilename());
            assertEquals(InputType.PDF, input.getType());
        }

        @Test
        @DisplayName("Parses JPEG URL")
        void parsesJpegUrl() {
            AIFileInput input = AIFileInput.fromUrl("https://cdn.example.com/image.jpeg");

            assertEquals("image.jpeg", input.getFilename());
            assertEquals(InputType.IMAGE, input.getType());
        }

        @Test
        @DisplayName("Parses WebP URL")
        void parsesWebpUrl() {
            AIFileInput input = AIFileInput.fromUrl("https://example.com/photo.webp");

            assertEquals("photo.webp", input.getFilename());
            assertEquals(InputType.IMAGE, input.getType());
        }
    }

    // ---------------------------------------------------------------
    // 5. fromClasspath
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromClasspath")
    class FromClasspath {

        @Test
        @DisplayName("Throws on nonexistent classpath resource")
        void throwsOnMissingResource() {
            assertThrows(IllegalArgumentException.class, () ->
                    AIFileInput.fromClasspath("/nonexistent/image.png")
            );
        }
    }
}
