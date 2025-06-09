//package org.example.database;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.transaction.Transactional;
//import org.example.DataBaseRepositories.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class DataInitializer {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private CourseRepository courseRepository;
//
//    @Autowired
//    private CourseGroupRepository courseGroupRepository;
//
//    @Autowired
//    private CourseFileRepository courseFileRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Autowired
//    private QuizRepository quizRepository;
//
//    @Autowired
//    private QuizQuestionRepository quizQuestionRepository;
//
//    private Long courseBHPId; // Przechowuje ID kursu administracyjno-biurowego
//    private Long courseBHP2Id; // Przechowuje ID kursu dydaktycznego
//
//    @PostConstruct
//    @Transactional
//    public void init() {
//        initUsers();
//        initCourseGroupsAndCourses();
//        initCourseFiles();
//        initQuizzes();
//        initAdditionalQuizzes();
//    }
//
//    private void initUsers() {
//        if (userRepository.findByUsername("user").isEmpty()) {
//            User student = new User();
//            student.setUsername("user");
//            student.setPassword(passwordEncoder.encode("user"));
//            student.setRole(UserRole.STUDENT);
//            userRepository.save(student);
//        }
//        if (userRepository.findByUsername("user3").isEmpty()) {
//            User admin = new User();
//            admin.setUsername("user3");
//            admin.setPassword(passwordEncoder.encode("user"));
//            admin.setRole(UserRole.ADMIN);
//            userRepository.save(admin);
//        }
//        if (userRepository.findByUsername("teacher").isEmpty()) {
//            User teacher = new User();
//            teacher.setUsername("teacher");
//            teacher.setPassword(passwordEncoder.encode("teacher123"));
//            teacher.setRole(UserRole.TEACHER);
//            userRepository.save(teacher);
//        }
//        if (userRepository.findByUsername("teacher1").isEmpty()) {
//            User teacher = new User();
//            teacher.setUsername("teacher1");
//            teacher.setPassword(passwordEncoder.encode("teacher123"));
//            teacher.setRole(UserRole.TEACHER);
//            userRepository.save(teacher);
//        }
//    }
//
//    private void initCourseGroupsAndCourses() {
//        if (courseRepository.count() == 0) {
//            User teacher = userRepository.findByUsername("teacher")
//                    .orElseThrow(() -> new RuntimeException("Teacher 'teacher' not found"));
//            User teacher1 = userRepository.findByUsername("teacher1")
//                    .orElseThrow(() -> new RuntimeException("Teacher 'teacher1' not found"));
//
//            CourseGroup courseBHP1 = new CourseGroup();
//            courseBHP1.setName("Szkolenie BHP adm. biu.");
//            courseBHP1.setDescription("Obowiązkowe szkolenie okresowe dla pracowników administracyjno-biurowych");
//            courseBHP1.setTeacher(teacher1);
//            courseGroupRepository.save(courseBHP1);
//
//            Course courseBHP = new Course();
//            courseBHP.setCourseName("BHP dla Pracowników administracyjno-biurowych 2025");
//            courseBHP.setDescription("Obowiązkowe szkolenie okresowe dla pracowników administracyjno-biurowych");
//            courseBHP.setAccessKey("BHP-ADM25");
//            courseBHP.setTeacher(teacher1);
//            courseBHP.setCourseGroup(courseBHP1);
//            courseRepository.save(courseBHP);
//            courseBHPId = courseBHP.getId(); // Zapisujemy ID kursu
//
//            CourseGroup bhpGroup = new CourseGroup();
//            bhpGroup.setName("Szkolenia BHP dyd");
//            bhpGroup.setDescription("Obowiązkowe szkolenie okresowe dla pracowników dydaktycznych.");
//            bhpGroup.setTeacher(teacher1);
//            courseGroupRepository.save(bhpGroup);
//
//            Course courseBHP2 = new Course();
//            courseBHP2.setCourseName("BHP dla Pracowników dydaktycznych 2025");
//            courseBHP2.setDescription("Obowiązkowe szkolenie okresowe dla pracowników dydaktycznych.");
//            courseBHP2.setAccessKey("BHP-DYD25");
//            courseBHP2.setTeacher(teacher1);
//            courseBHP2.setCourseGroup(bhpGroup);
//            courseRepository.save(courseBHP2);
//            courseBHP2Id = courseBHP2.getId(); // Zapisujemy ID kursu
//        }
//    }
//
//    private void initQuizzes() {
//        if (quizRepository.count() == 0) {
//            Course course = courseRepository.findById(courseBHP2Id)
//                    .orElseThrow(() -> new RuntimeException("Kurs o ID " + courseBHP2Id + " nie został znaleziony."));
//
//            Quiz quiz = new Quiz("Test końcowy BHP", "Sprawdzenie wiedzy ze szkolenia BHP", course, 20);
//            quizRepository.save(quiz);
//
//            List<QuizQuestion> questions = List.of(
//                    new QuizQuestion("Do gaszenia urządzeń elektrycznych pod napięciem należy stosować:", "multiple_choice",
//                            Map.of("A", "Hydrant wewnętrzny", "B", "Gaśnicę proszkową", "C", "Gaśnice płynową"), "B", quiz),
//                    new QuizQuestion("Pożary zaliczone do grupy B to:", "multiple_choice",
//                            Map.of("A", "Pożary ciał stałych", "B", "Pożary cieczy", "C", "Pożary gazów"), "B", quiz),
//                    new QuizQuestion("Znak „wyjście ewakuacyjne” powinien być umieszczony:", "multiple_choice",
//                            Map.of("A", "Na każdych drzwiach wyjściowych", "B", "Na drzwiach wyjściowych z sal wykładowych", "C", "Na drzwiach przy wyjściu z budynku"), "C", quiz),
//                    new QuizQuestion("Natężenie oświetlenia w pomieszczeniach gdzie są stanowiska pracy z monitorami ekranowymi wynosi:", "multiple_choice",
//                            Map.of("A", "200 lx", "B", "300 lx", "C", "500 lx"), "B", quiz),
//                    new QuizQuestion("Temperatura na stanowisku biurowym – przy komputerze powinna wynosić nie mniej niż:", "multiple_choice",
//                            Map.of("A", "14oC", "B", "18oC", "C", "24oC"), "B", quiz),
//                    new QuizQuestion("Rodzajem wentylacji miejscowej mechanicznej jest:", "multiple_choice",
//                            Map.of("A", "Dygestorium", "B", "Infiltracja", "C", "Aeracja"), "A", quiz),
//                    new QuizQuestion("Wysokość pomieszczenia stałej pracy nie może być mniejsza niż:", "multiple_choice",
//                            Map.of("A", "3,3 m ...", "B", "3 m ...", "C", "2,5 m ..."), "B", quiz),
//                    new QuizQuestion("Monitor komputera powinien być tak ustawiony, aby:", "multiple_choice",
//                            Map.of("A", "Środek ekranu ...", "B", "Górna krawędź ekranu ...", "C", "Środek ekranu powyżej oczu ..."), "B", quiz),
//                    new QuizQuestion("Jedną z kar dyscyplinarnych nauczyciela akademickiego jest:", "multiple_choice",
//                            Map.of("A", "Upomnienie z wpisem do akt", "B", "Grzywna lub mandat", "C", "Pozbawienie prawa ..."), "A", quiz),
//                    new QuizQuestion("Osoba odpowiedzialna za BHP podlega karze:", "multiple_choice",
//                            Map.of("A", "Upomnienia", "B", "Nagany", "C", "Grzywny"), "C", quiz),
//                    new QuizQuestion("Wypadkiem przy pracy jest:", "multiple_choice",
//                            Map.of("A", "Negatywny wpływ środowiska ...", "B", "Nagłe zdarzenie ...", "C", "Każdy uraz na terenie uczelni"), "B", quiz),
//                    new QuizQuestion("Wypadek na konferencji zagranicznej to:", "multiple_choice",
//                            Map.of("A", "Wypadek przy pracy", "B", "Szczególne okoliczności", "C", "Traktowany na równi z wypadkiem przy pracy"), "C", quiz),
//                    new QuizQuestion("Podstawowym dokumentem do świadczeń powypadkowych:", "multiple_choice",
//                            Map.of("A", "Karta wypadku", "B", "Protokół powypadkowy", "C", "Zaświadczenie lekarskie"), "B", quiz),
//                    new QuizQuestion("Prowadzący zajęcia w laboratorium musi:", "multiple_choice",
//                            Map.of("A", "Zgłosić zagrożenie i pracować", "B", "Zaznajomić uczestników z BHP", "C", "Stosować instrukcję tylko na swoim stanowisku"), "B", quiz),
//                    new QuizQuestion("Wstęp do laboratorium z innej jednostki:", "multiple_choice",
//                            Map.of("A", "Za zgodą kierownika labu", "B", "Za zgodą kierownika jednostki", "C", "Za zgodą BHP lub PIP"), "B", quiz),
//                    new QuizQuestion("Czynnik niebezpieczny w środowisku pracy:", "multiple_choice",
//                            Map.of("A", "Prąd elektryczny", "B", "Pył azbestu", "C", "Wibracja"), "A", quiz),
//                    new QuizQuestion("Prace szczególnie niebezpieczne wymagają zgody gdy:", "multiple_choice",
//                            Map.of("A", "Wysokość >1m", "B", "Spawanie w pomieszczeniu", "C", "Obie odpowiedzi prawidłowe"), "C", quiz),
//                    new QuizQuestion("Pierwsza pomoc przy ataku padaczki:", "multiple_choice",
//                            Map.of("A", "Leży na plecach – chronić głowę", "B", "Leży na brzuchu – przekładamy na plecy", "C", "Obie odpowiedzi prawidłowe"), "A", quiz),
//                    new QuizQuestion("Zadławienie osoby dorosłej – co najpierw:", "multiple_choice",
//                            Map.of("A", "Wyjęcie ciała i 5 uderzeń", "B", "Wyjęcie i 5 uciśnięć brzucha", "C", "Obie odpowiedzi"), "A", quiz),
//                    new QuizQuestion("Resuscytacja osoby porażonej prądem:", "multiple_choice",
//                            Map.of("A", "5 wdechów, 30 uciśnięć", "B", "30 uciśnięć, 2 wdechy", "C", "Nie ma to znaczenia"), "B", quiz)
//            );
//
//            quizQuestionRepository.saveAll(questions);
//        }
//    }
//
//    private void initAdditionalQuizzes() {
//        Course course = courseRepository.findById(courseBHPId)
//                .orElseThrow(() -> new RuntimeException("Kurs o ID " + courseBHPId + " nie został znaleziony."));
//
//        if (quizRepository.findByCourseId(courseBHPId).isEmpty()) {
//            Quiz additionalQuiz = new Quiz("Test BHP dla pracowników administracyjno-biurowych",
//                    "Sprawdzenie wiedzy z zakresu BHP dla pracowników biurowych", course, 21);
//            quizRepository.save(additionalQuiz);
//
//            List<QuizQuestion> additionalQuestions = List.of(
//                    new QuizQuestion("Zatrudnienie kobiet ciężarnych na stanowiskach z monitorami ekranowymi jest:", "multiple_choice",
//                            Map.of("A", "dopuszczalne do trzeciego miesiąca ciąży włącznie",
//                                    "B", "niedopuszczalne",
//                                    "C", "dopuszczalne w łącznym czasie nieprzekraczającym 8 godzin na dobę, przy czym czas spędzony przy obsłudze monitora ekranowego nie może jednorazowo przekraczać 50 minut, po którym to czasie powinna nastąpić co najmniej 10-minutowa przerwa, wliczana do czasu pracy"),
//                            "C", additionalQuiz),
//                    new QuizQuestion("Najmniejsze dopuszczalne średnie natężenie oświetlenia w pomieszczeniach gdzie są stanowiska pracy z monitorami ekranowymi wynosi:", "multiple_choice",
//                            Map.of("A", "200 lx", "B", "300 lx", "C", "500 lx"), "B", additionalQuiz),
//                    new QuizQuestion("Maksymalny dopuszczalny poziom hałasu w pomieszczeniu biurowym to:", "multiple_choice",
//                            Map.of("A", "55 dB", "B", "65 dB", "C", "85 dB"), "B", additionalQuiz),
//                    new QuizQuestion("Do gaszenia urządzeń pod napięciem należy stosować:", "multiple_choice",
//                            Map.of("A", "gaśnicę proszkową", "B", "gaśnicę pianową", "C", "gaśnicę śniegową"), "A", additionalQuiz),
//                    new QuizQuestion("Monitor komputera powinien być tak ustawiony, aby:", "multiple_choice",
//                            Map.of("A", "środek ekranu znajdował się na wysokości oczu operatora",
//                                    "B", "górna krawędź ekranu znajdowała się na wysokości oczu operatora",
//                                    "C", "środek ekranu znajdował się powyżej oczu operatora"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Podczas pracy przy komputerze pracownik powinien sobie robić przerwy:", "multiple_choice",
//                            Map.of("A", "co najmniej 15 minut po 4 godzinach pracy",
//                                    "B", "5 minut po każdej godzinie intensywnej pracy",
//                                    "C", "częstotliwość przerw należy uzależnić od wskazań lekarskich"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Udzielając pomocy osobie, która doznała ataku epilepsji:", "multiple_choice",
//                            Map.of("A", "kładziemy osobę w pozycji bocznej i wzywamy pogotowie",
//                                    "B", "chronimy głowę poszkodowanego, zapewniamy dostęp świeżego powietrza, nie podajemy leków i płynów",
//                                    "C", "kładziemy osobę na wznak, między zęby wkładamy twardy przedmiot i wzywamy pogotowie"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Udzielając pomocy osobie, która się zadławiła należy zacząć od:", "multiple_choice",
//                            Map.of("A", "próby wyjęcia ciała obcego, a następnie wykonać od 5-ciu uderzeń w plecy między łopatki",
//                                    "B", "próby wyjęcia ciała obcego oraz wykonać od 5-ciu uciśnięć okolicy nadbrzusza",
//                                    "C", "obie odpowiedzi są prawidłowe"),
//                            "A", additionalQuiz),
//                    new QuizQuestion("Celem okresowych badań lekarskich jest:", "multiple_choice",
//                            Map.of("A", "ustalenie, czy pod wpływem warunków pracy zachodzą w stanie zdrowia pracownika, niekorzystne zmiany",
//                                    "B", "ustalenie, czy pracownik jest całkowicie zdrowy",
//                                    "C", "ustalenie, czy zdolny jest do pracy po chorobie"),
//                            "A", additionalQuiz),
//                    new QuizQuestion("Kontrolnym badaniom lekarskim poddawany jest pracownik, który:", "multiple_choice",
//                            Map.of("A", "chorował dłużej niż 30 dni",
//                                    "B", "był nieobecny w pracy powyżej 30 dni",
//                                    "C", "uległ wypadkowi przy pracy"),
//                            "A", additionalQuiz),
//                    new QuizQuestion("W razie konieczności ewakuacji pracownik ma obowiązek:", "multiple_choice",
//                            Map.of("A", "biegiem opuścić budynek",
//                                    "B", "wsiąść do windy i jak najszybciej zjechać na dół, po czym opuścić budynek",
//                                    "C", "bez paniki wyjść z budynku klatką schodową i napotkane osoby informować o konieczności ewakuacji"),
//                            "C", additionalQuiz),
//                    new QuizQuestion("Do czynników uciążliwych na stanowisku monitora ekranowego należą:", "multiple_choice",
//                            Map.of("A", "obciążenie układu mięśniowo-szkieletowego, obciążenie wzroku i obciążenie psychiczne",
//                                    "B", "obciążenie układu mięśniowo-szkieletowego i obciążenie wzroku",
//                                    "C", "obciążenie wzroku"),
//                            "A", additionalQuiz),
//                    new QuizQuestion("Powierzchnia użytkowa przysługująca jednemu pracownikowi wynosi:", "multiple_choice",
//                            Map.of("A", "13m3 objętości pomieszczenia i 2m2 podłogi",
//                                    "B", "15m3",
//                                    "C", "12m2"),
//                            "A", additionalQuiz),
//                    new QuizQuestion("W razie porażenia prądem współpracownika należy:", "multiple_choice",
//                            Map.of("A", "położyć osobę w pozycji bocznej i wzywać pogotowie",
//                                    "B", "nie dotykać skóry poszkodowanego, jeśli nie ma on kontaktu z napięciem oraz nie podawać leków i płynów",
//                                    "C", "odciąć dopływ prądu (bezpiecznik lub wtyczka z kontaktu) lub odsunąć poszkodowanego od źródła prądu za pomocą drewnianego przedmiotu, stojąc na suchej macie gumowej, książce lub złożonej gazecie"),
//                            "C", additionalQuiz),
//                    new QuizQuestion("Pracownik za nie przestrzeganie przepisów i zasad bezpieczeństwa i higieny pracy podlega karze:", "multiple_choice",
//                            Map.of("A", "pieniężnej", "B", "pozbawienia wolności", "C", "zwolnienia dyscyplinarnego"),
//                            "A", additionalQuiz),
//                    new QuizQuestion("Gdy pracownik administracyjny uległ wypadkowi w czasie podróży służbowej, to uległ:", "multiple_choice",
//                            Map.of("A", "wypadkowi przy pracy",
//                                    "B", "wypadkowi traktowanemu na równi z wypadkiem przy pracy",
//                                    "C", "wypadkowi w okresie ubezpieczenia"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Za stan bezpieczeństwa i higieny pracy w zakładzie odpowiedzialność ponosi:", "multiple_choice",
//                            Map.of("A", "Służba BHP", "B", "Pracodawca", "C", "Państwowa Inspekcja Pracy"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Pożary grupy B, to pożary:", "multiple_choice",
//                            Map.of("A", "ciał stałych", "B", "tłuszczy kuchennych", "C", "cieczy palnych"),
//                            "C", additionalQuiz),
//                    new QuizQuestion("Pracodawca jest zobowiązany zapewnić pracownikom napoje chłodzące gdy temperatura w pomieszczeniu biurowym przekracza:", "multiple_choice",
//                            Map.of("A", "25oC", "B", "28oC", "C", "30oC"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Wypadkiem przy pracy jest:", "multiple_choice",
//                            Map.of("A", "negatywny wpływ środowiska pracy na organizm ludzki",
//                                    "B", "nagłe zdarzenie, wywołane przyczyną zewnątrzną, mające związek z pracą i powodujące uraz lub śmierć",
//                                    "C", "każdy uraz doznany przez pracownika na terenie uczelni"),
//                            "B", additionalQuiz),
//                    new QuizQuestion("Podstawowym dokumentem uprawniającym do świadczeń z tytułu wypadku przy pracy jest:", "multiple_choice",
//                            Map.of("A", "karta wypadku przy pracy", "B", "protokół powypadkowy", "C", "zaświadczenie lekarskie o niezdolności do pracy z tytułu wypadku"),
//                            "B", additionalQuiz)
//            );
//
//            quizQuestionRepository.saveAll(additionalQuestions);
//        }
//    }
//
//    private void initCourseFiles() {
//        Course bhpCourse = courseRepository.findById(courseBHP2Id)
//                .orElseThrow(() -> new RuntimeException("Kurs o ID " + courseBHP2Id + " nie został wybrany!"));
//
//        courseFileRepository.save(new CourseFile("I. Regulacje prawne.doc", "/files/I. Regulacje prawne.doc", bhpCourse));
//        courseFileRepository.save(new CourseFile("II. Ocena zagrożeń.doc", "/files/II. Ocena zagrożeń.doc", bhpCourse));
//        courseFileRepository.save(new CourseFile("III. Organ. stanowisk pracy.doc", "/files/III. Organ. stanowisk pracy.doc", bhpCourse));
//        courseFileRepository.save(new CourseFile("IV. Wypadki, choroby zaw..doc", "/files/IV. Wypadki, choroby zaw..doc", bhpCourse));
//        courseFileRepository.save(new CourseFile("V. Ochrona ppoż..doc", "/files/V. Ochrona ppoż..doc", bhpCourse));
//        courseFileRepository.save(new CourseFile("AED + BHP2.docx", "/files/AED + BHP2.docx", bhpCourse));
//        courseFileRepository.save(new CourseFile("Ma. Charakterystyczne wypadki w PŚk..docx", "/files/Ma. Charakterystyczne wypadki w PŚk..docx", bhpCourse));
//        courseFileRepository.save(new CourseFile("System udzielania I pomocy w PŚk.docx", "/files/System udzielania I pomocy w PŚk.docx", bhpCourse));
//
//        Course bhpCourse1 = courseRepository.findById(courseBHPId)
//                .orElseThrow(() -> new RuntimeException("Kurs o ID " + courseBHPId + " nie został wybrany!"));
//        courseFileRepository.save(new CourseFile("I. Regulacje prawne.doc", "/files/Mat. szkol. admini. biurowi/I. Regulacje prawne.doc", bhpCourse1));
//        courseFileRepository.save(new CourseFile("II. Ocena zagrożeń.doc", "/files/Mat. szkol. admini. biurowi/II. Ocena zagrożeń.doc", bhpCourse1));
//        courseFileRepository.save(new CourseFile("III. Organ. stanowisk pracy.doc", "/files/Mat. szkol. admini. biurowi/III. Organ. stanowisk pracy.doc", bhpCourse1));
//        courseFileRepository.save(new CourseFile("IV. Wypadki, choroby zaw..doc", "/files/Mat. szkol. admini. biurowi/IV. Wypadki, choroby zaw..doc", bhpCourse1));
//        courseFileRepository.save(new CourseFile("V. Ochrona ppoż..doc", "/files/Mat. szkol. admini. biurowi/V. Ochrona ppoż..doc", bhpCourse1));
//        courseFileRepository.save(new CourseFile("AED + BHP2.docx", "/files/Mat. szkol. admini. biurowi/AED + BHP2.docx", bhpCourse1));
//        courseFileRepository.save(new CourseFile("Ma. Charakterystyczne wypadki w PŚk..docx", "/files/Mat. szkol. admini. biurowi/IVa. Charakterystyczne wypadki w PŚk..docx", bhpCourse1));
//        courseFileRepository.save(new CourseFile("System udzielania I pomocy w PŚk.docx", "/files/Mat. szkol. admini. biurowi/System udzielania I pomocy w PŚk.docx", bhpCourse1));
//
//
//    }
//}