package org.example.database;

import jakarta.annotation.PostConstruct;
import org.example.DataBaseRepositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    // DODANO REPOZYTORIUM DLA GRUP KURSÓW
    @Autowired
    private CourseGroupRepository courseGroupRepository;

    @Autowired
    private CourseFileRepository courseFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @PostConstruct
    public void init() {
        // Inicjalizacja w odpowiedniej kolejności
        initUsers();
        initCourseGroupsAndCourses();
        initCourseFiles();
        initQuizzes();
    }

    private void initUsers() {
        if (userRepository.findByUsername("user").isEmpty()) {
            User student = new User();
            student.setUsername("user");
            student.setPassword(passwordEncoder.encode("user"));
            student.setRole(UserRole.STUDENT);
            userRepository.save(student);
        }
        if (userRepository.findByUsername("user3").isEmpty()) {
            User admin = new User();
            admin.setUsername("user3");
            admin.setPassword(passwordEncoder.encode("user"));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("teacher").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole(UserRole.TEACHER);
            userRepository.save(teacher);
        }
        if (userRepository.findByUsername("teacher1").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher1");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole(UserRole.TEACHER);
            userRepository.save(teacher);
        }
    }

    // ZASTĄPIONO STARE METODY JEDNĄ, POPRAWNĄ METODĄ
    private void initCourseGroupsAndCourses() {
        // Upewniamy się, że dane są dodawane tylko raz, sprawdzając repozytorium grup
        if (courseGroupRepository.count() == 0) {
            // Pobieramy nauczycieli
            User teacher = userRepository.findByUsername("teacher")
                    .orElseThrow(() -> new RuntimeException("Teacher 'teacher' not found"));
            User teacher1 = userRepository.findByUsername("teacher1")
                    .orElseThrow(() -> new RuntimeException("Teacher 'teacher1' not found"));

            // --- GRUPA 1: KURSY JAVY ---
            CourseGroup javaGroup = new CourseGroup();
            javaGroup.setName("Szkolenie BHP");
            javaGroup.setDescription("Nauka zasad BHP.");
            javaGroup.setTeacher(teacher);
            courseGroupRepository.save(javaGroup); // Zapisz grupę, aby uzyskać ID

            // Kurs 1 w grupie Java
            Course course1 = new Course();
            course1.setCourseName("Java Basics - Edycja Jesień 2024");
            course1.setDescription("Wprowadzenie do programowania w języku Java.");
            course1.setAccessKey("JAVA-FALL24");
            course1.setTeacher(teacher);
            course1.setCourseGroup(javaGroup); // Przypisanie kursu do grupy
            courseRepository.save(course1);



            // --- GRUPA 2: SZKOLENIA BHP ---
            CourseGroup bhpGroup = new CourseGroup();
            bhpGroup.setName("Szkolenia BHP");
            bhpGroup.setDescription("Okresowe szkolenia z Bezpieczeństwa i Higieny Pracy.");
            bhpGroup.setTeacher(teacher1);
            courseGroupRepository.save(bhpGroup);

            // Kurs 1 w grupie BHP
            Course courseBHP = new Course();
            courseBHP.setCourseName("BHP dla Pracowników Biurowych 2025");
            courseBHP.setDescription("Obowiązkowe szkolenie okresowe dla pracowników administracyjno-biurowych.");
            courseBHP.setAccessKey("BHP-ADM25");
            courseBHP.setTeacher(teacher1);
            courseBHP.setCourseGroup(bhpGroup);
            courseRepository.save(courseBHP);
        }
    }

    private void initQuizzes() {
        if (quizRepository.count() == 0) {
            Course course = courseRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("Kurs o ID 2 nie został znaleziony."));

            Quiz quiz = new Quiz("Test końcowy BHP", "Sprawdzenie wiedzy ze szkolenia BHP", course, 20);
            quizRepository.save(quiz);

            List<QuizQuestion> questions = List.of(
                    new QuizQuestion("Do gaszenia urządzeń elektrycznych pod napięciem należy stosować:", "multiple_choice",
                            Map.of("A", "Hydrant wewnętrzny", "B", "Gaśnicę proszkową", "C", "Gaśnice płynową"), "B", quiz),
                    new QuizQuestion("Pożary zaliczone do grupy B to:", "multiple_choice",
                            Map.of("A", "Pożary ciał stałych", "B", "Pożary cieczy", "C", "Pożary gazów"), "B", quiz),
                    new QuizQuestion("Znak „wyjście ewakuacyjne” powinien być umieszczony:", "multiple_choice",
                            Map.of("A", "Na każdych drzwiach wyjściowych", "B", "Na drzwiach wyjściowych z sal wykładowych", "C", "Na drzwiach przy wyjściu z budynku"), "C", quiz),
                    new QuizQuestion("Natężenie oświetlenia w pomieszczeniach gdzie są stanowiska pracy z monitorami ekranowymi wynosi:", "multiple_choice",
                            Map.of("A", "200 lx", "B", "300 lx", "C", "500 lx"), "B", quiz),
                    new QuizQuestion("Temperatura na stanowisku biurowym – przy komputerze powinna wynosić nie mniej niż:", "multiple_choice",
                            Map.of("A", "14oC", "B", "18oC", "C", "24oC"), "B", quiz),
                    new QuizQuestion("Rodzajem wentylacji miejscowej mechanicznej jest:", "multiple_choice",
                            Map.of("A", "Dygestorium", "B", "Infiltracja", "C", "Aeracja"), "A", quiz),
                    new QuizQuestion("Wysokość pomieszczenia stałej pracy nie może być mniejsza niż:", "multiple_choice",
                            Map.of("A", "3,3 m ...", "B", "3 m ...", "C", "2,5 m ..."), "B", quiz),
                    new QuizQuestion("Monitor komputera powinien być tak ustawiony, aby:", "multiple_choice",
                            Map.of("A", "Środek ekranu ...", "B", "Górna krawędź ekranu ...", "C", "Środek ekranu powyżej oczu ..."), "B", quiz),
                    new QuizQuestion("Jedną z kar dyscyplinarnych nauczyciela akademickiego jest:", "multiple_choice",
                            Map.of("A", "Upomnienie z wpisem do akt", "B", "Grzywna lub mandat", "C", "Pozbawienie prawa ..."), "A", quiz),
                    new QuizQuestion("Osoba odpowiedzialna za BHP podlega karze:", "multiple_choice",
                            Map.of("A", "Upomnienia", "B", "Nagany", "C", "Grzywny"), "C", quiz),
                    new QuizQuestion("Wypadkiem przy pracy jest:", "multiple_choice",
                            Map.of("A", "Negatywny wpływ środowiska ...", "B", "Nagłe zdarzenie ...", "C", "Każdy uraz na terenie uczelni"), "B", quiz),
                    new QuizQuestion("Wypadek na konferencji zagranicznej to:", "multiple_choice",
                            Map.of("A", "Wypadek przy pracy", "B", "Szczególne okoliczności", "C", "Traktowany na równi z wypadkiem przy pracy"), "C", quiz),
                    new QuizQuestion("Podstawowy dokument do świadczeń powypadkowych:", "multiple_choice",
                            Map.of("A", "Karta wypadku", "B", "Protokół powypadkowy", "C", "Zaświadczenie lekarskie"), "B", quiz),
                    new QuizQuestion("Prowadzący zajęcia w laboratorium musi:", "multiple_choice",
                            Map.of("A", "Zgłosić zagrożenie i pracować", "B", "Zaznajomić uczestników z BHP", "C", "Stosować instrukcję tylko na swoim stanowisku"), "B", quiz),
                    new QuizQuestion("Wstęp do laboratorium z innej jednostki:", "multiple_choice",
                            Map.of("A", "Za zgodą kierownika labu", "B", "Za zgodą kierownika jednostki", "C", "Za zgodą BHP lub PIP"), "B", quiz),
                    new QuizQuestion("Czynnik niebezpieczny w środowisku pracy:", "multiple_choice",
                            Map.of("A", "Prąd elektryczny", "B", "Pył azbestu", "C", "Wibracja"), "A", quiz),
                    new QuizQuestion("Prace szczególnie niebezpieczne wymagają zgody gdy:", "multiple_choice",
                            Map.of("A", "Wysokość >1m", "B", "Spawanie w pomieszczeniu", "C", "Obie odpowiedzi prawidłowe"), "C", quiz),
                    new QuizQuestion("Pierwsza pomoc przy ataku padaczki:", "multiple_choice",
                            Map.of("A", "Leży na plecach – chronić głowę", "B", "Leży na brzuchu – przekładamy na plecy", "C", "Obie odpowiedzi prawidłowe"), "A", quiz),
                    new QuizQuestion("Zadławienie osoby dorosłej – co najpierw:", "multiple_choice",
                            Map.of("A", "Wyjęcie ciała i 5 uderzeń", "B", "Wyjęcie i 5 uciśnięć brzucha", "C", "Obie odpowiedzi"), "A", quiz),
                    new QuizQuestion("Resuscytacja osoby porażonej prądem:", "multiple_choice",
                            Map.of("A", "5 wdechów, 30 uciśnięć", "B", "30 uciśnięć, 2 wdechy", "C", "Nie ma to znaczenia"), "B", quiz)
            );

            quizQuestionRepository.saveAll(questions);
        }
    }





    private void initCourseFiles() {
        if (courseFileRepository.count() == 0) {
            // ZAKTUALIZOWANO NAZWĘ KURSU, ABY PASOWAŁA DO NOWYCH DANYCH
            Course javaCourse = courseRepository.findByCourseName("Java Basics - Edycja Jesień 2024")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Java Basics - Edycja Jesień 2024' nie znaleziony!"));

            courseFileRepository.save(new CourseFile("java_intro.pdf", "/files/java_intro.pdf", javaCourse));
            courseFileRepository.save(new CourseFile("java_syntax.pdf", "/files/java_syntax.pdf", javaCourse));
        }
    }
}
 //pliki do kursu nauczycieli
//INSERT INTO course_file (file_name, file_url, course_id) VALUES
//('I. Regulacje prawne.doc', '/files/I. Regulacje prawne.doc', 2),
//        ('II. Ocena zagrożeń.doc', '/files/II. Ocena zagrożeń.doc', 2),
//        ('III. Organ. stanowisk pracy.doc', '/files/III. Organ. stanowisk pracy.doc', 2),
//        ('IV. Wypadki, choroby zaw..doc', '/files/IV. Wypadki, choroby zaw..doc', 2),
//        ('V. Ochrona ppoż..doc', '/files/V. Ochrona ppoż..doc', 2),
//        ('AED + BHP2.docx', '/files/AED + BHP2.docx', 2),
//        ('Ma. Charakterystyczne wypadki w PŚk..docx', '/files/Ma. Charakterystyczne wypadki w PŚk..docx', 2),
//        ('System udzielania I pomocy w PŚk.docx', '/files/System udzielania I pomocy w PŚk.docx', 2);
