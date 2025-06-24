package org.example;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.example.database.Quiz;
import org.example.database.QuizAnswer;
import org.example.database.QuizResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Usługa odpowiedzialna za generowanie raportów PDF z wynikami quizów.
 * Wykorzystuje bibliotekę iText7 do tworzenia dokumentów PDF.
 */
@Service
public class PdfGenerationService {

    /**
     * Generuje plik PDF zawierający szczegółowe wyniki dla danego quizu.
     * Raport zawiera podsumowanie quizu oraz listę wyników poszczególnych studentów,
     * wraz z ich odpowiedziami na pytania.
     *
     * @param quiz Obiekt Quiz, dla którego generowany jest raport.
     * @param results Lista obiektów QuizResult zawierających wyniki studentów.
     * @return ByteArrayInputStream zawierający wygenerowany plik PDF.
     */
    public ByteArrayInputStream generateQuizResultsPdf(Quiz quiz, List<QuizResult> results) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
        Document document = new Document(pdfDoc);

        // Nagłówek dokumentu
        document.add(new Paragraph("Raport wyników dla quizu: " + quiz.getTitle())
                .setBold().setFontSize(20).setMarginBottom(5));
        document.add(new Paragraph("Wygenerowano: " + java.time.LocalDate.now())
                .setFontSize(10));
        document.add(new Paragraph("\n"));

        // Sekcja dla każdego studenta
        for (QuizResult result : results) {
            String studentName = result.getUser() != null ? result.getUser().getUsername() : "Nieznany Użytkownik";
            document.add(new Paragraph("Student: " + studentName)
                    .setBold().setFontSize(14));
            document.add(new Paragraph(String.format("Data ukończenia: %s",
                    result.getCompletionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))))
                    .setFontSize(10));
            document.add(new Paragraph(String.format("Wynik: %d / %d (%.2f%%)",
                    result.getCorrectAnswers(), result.getTotalQuestions(),
                    (double) result.getCorrectAnswers() * 100 / result.getTotalQuestions())));

            document.add(new Paragraph("\n").setFontSize(4)); // Mały odstęp

            // Tabela z odpowiedziami
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));

            // Nagłówki tabeli
            table.addHeaderCell(new Cell().add(new Paragraph("Pytanie").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Odpowiedź studenta").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Poprawna odpowiedź").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));

            List<QuizAnswer> answers = result.getQuizAnswers();
            for (QuizAnswer answer : answers) {
                table.addCell(answer.getQuestion().getQuestionText());
                table.addCell(answer.getUserAnswer());
                table.addCell(answer.getQuestion().getCorrectAnswer());
                table.addCell(answer.isCorrect() ? "OK" : "Błąd");
            }
            document.add(table);
            document.add(new Paragraph("\n\n")); // Większy odstęp między tabelami studentów
        }

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }
}