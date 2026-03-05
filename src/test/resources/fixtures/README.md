# Test Fixture

Place a tiny valid MP3 file at:

`src/test/resources/fixtures/tiny-valid.mp3`

The end-to-end test `addOrReplaceArtworkEndToEndPreservesExistingMetadata` in
`src/test/java/com/example/singsongartwork/Mp3MetadataServiceTest.java` uses this file.

If the file is missing, the test is skipped.

