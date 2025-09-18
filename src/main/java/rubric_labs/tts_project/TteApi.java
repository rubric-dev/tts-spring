package rubric_labs.tts_project;


import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RequestMapping(("/tts"))
@RestController
public class TteApi {

    @PostMapping(value = "",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void readPdf(@RequestPart("file") MultipartFile file) throws IOException {
        // MultipartFile → InputStream → PDFBox
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // 간단히 로그/콘솔에 출력
            System.out.println("===== PDF Extracted Text =====");
            System.out.println(text);
            System.out.println("==============================");
        }
    }
}
