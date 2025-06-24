package org.example.database;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.example.DataBaseRepositories.*;
import org.example.database.*; // Upewnij się, że importujesz swoje klasy modeli (User, Course, etc.)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DataInitializer {

    // Repozytoria i inne zależności wstrzykiwane przez Spring
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CourseGroupRepository courseGroupRepository;
    @Autowired private CourseFileRepository courseFileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizQuestionRepository quizQuestionRepository;

    // Klucze dostępowe kursów używane jako unikalne identyfikatory biznesowe
    private static final String BHP_ADM_ACCESS_KEY = "BHP-ADM25";
    private static final String BHP_DYD_ACCESS_KEY = "BHP-DYD25";

    @PostConstruct
    @Transactional
    public void init() {
        initUsers();
        initCourseGroupsAndCourses();
        initCourseFiles();
        initQuizzes();
    }

    /**
     * Inicjalizuje domyślnych użytkowników, tylko jeśli nie istnieją w bazie.
     * Wykorzystuje metodę findByUsername z UserRepository.
     */
    private void initUsers() {
        createUserIfNotFound("user", "user", UserRole.STUDENT);
        createUserIfNotFound("user3", "user", UserRole.ADMIN);
        createUserIfNotFound("teacher", "teacher123", UserRole.TEACHER);
        createUserIfNotFound("teacher1", "teacher123", UserRole.TEACHER);
    }

    /**
     * Inicjalizuje grupy kursów i kursy.
     * Sprawdza istnienie obiektów przed ich utworzeniem.
     */
    private void initCourseGroupsAndCourses() {
        User teacher1 = userRepository.findByUsername("teacher1") //
                .orElseThrow(() -> new RuntimeException("Teacher 'teacher1' not found. Cannot initialize courses."));

        // Grupa kursów: Szkolenie BHP adm. biu.
        // Adaptacja: Sprawdzenie istnienia przez filtrowanie w pamięci, bo repozytorium nie ma findByName.
        CourseGroup courseGroupBhpAdm = courseGroupRepository.findAll().stream()
                .filter(g -> "Szkolenie BHP adm. biu.".equals(g.getName()))
                .findFirst()
                .orElseGet(() -> {
                    CourseGroup newGroup = new CourseGroup();
                    newGroup.setName("Szkolenie BHP adm. biu.");
                    newGroup.setDescription("Obowiązkowe szkolenie okresowe dla pracowników administracyjno-biurowych");
                    newGroup.setTeacher(teacher1);
                    return courseGroupRepository.save(newGroup);
                });

        // Kurs: BHP dla Pracowników administracyjno-biurowych 2025
        // Wykorzystuje metodę findByAccessKey z CourseRepository.
        courseRepository.findByAccessKey(BHP_ADM_ACCESS_KEY) //
                .orElseGet(() -> {
                    Course newCourse = new Course();
                    newCourse.setCourseName("BHP dla Pracowników administracyjno-biurowych 2025");
                    newCourse.setDescription("Obowiązkowe szkolenie okresowe dla pracowników administracyjno-biurowych");
                    newCourse.setAccessKey(BHP_ADM_ACCESS_KEY);
                    newCourse.setTeacher(teacher1);
                    newCourse.setCourseGroup(courseGroupBhpAdm);
                    return courseRepository.save(newCourse);
                });

        // Grupa kursów: Szkolenia BHP dyd
        // Adaptacja: Sprawdzenie istnienia przez filtrowanie w pamięci.
        CourseGroup courseGroupBhpDyd = courseGroupRepository.findAll().stream()
                .filter(g -> "Szkolenia BHP dyd".equals(g.getName()))
                .findFirst()
                .orElseGet(() -> {
                    CourseGroup newGroup = new CourseGroup();
                    newGroup.setName("Szkolenia BHP dyd");
                    newGroup.setDescription("Obowiązkowe szkolenie okresowe dla pracowników dydaktycznych.");
                    newGroup.setTeacher(teacher1);
                    return courseGroupRepository.save(newGroup);
                });

        // Kurs: BHP dla Pracowników dydaktycznych 2025
        // Wykorzystuje metodę findByAccessKey z CourseRepository.
        courseRepository.findByAccessKey(BHP_DYD_ACCESS_KEY) //
                .orElseGet(() -> {
                    Course newCourse = new Course();
                    newCourse.setCourseName("BHP dla Pracowników dydaktycznych 2025");
                    newCourse.setDescription("Obowiązkowe szkolenie okresowe dla pracowników dydaktycznych.");
                    newCourse.setAccessKey(BHP_DYD_ACCESS_KEY);
                    newCourse.setTeacher(teacher1);
                    newCourse.setCourseGroup(courseGroupBhpDyd);
                    return courseRepository.save(newCourse);
                });
    }

    /**
     * Inicjalizuje pliki kursów, sprawdzając, czy już nie istnieją dla danego kursu.
     */
    private void initCourseFiles() {
        Course courseAdm = courseRepository.findByAccessKey(BHP_ADM_ACCESS_KEY) //
                .orElseThrow(() -> new RuntimeException("Course with key " + BHP_ADM_ACCESS_KEY + " not found. Cannot add files."));
        Course courseDyd = courseRepository.findByAccessKey(BHP_DYD_ACCESS_KEY) //
                .orElseThrow(() -> new RuntimeException("Course with key " + BHP_DYD_ACCESS_KEY + " not found. Cannot add files."));

        // Pliki dla kursu dydaktycznego
        createCourseFileIfNotFound("I. Regulacje prawne.doc", "/files/I. Regulacje prawne.doc", courseDyd);
        createCourseFileIfNotFound("II. Ocena zagrożeń.doc", "/files/II. Ocena zagrożeń.doc", courseDyd);
        createCourseFileIfNotFound("III. Organ. stanowisk pracy.doc", "/files/III. Organ. stanowisk pracy.doc", courseDyd);
        createCourseFileIfNotFound("IV. Wypadki, choroby zaw..doc", "/files/IV. Wypadki, choroby zaw..doc", courseDyd);
        createCourseFileIfNotFound("V. Ochrona ppoż..doc", "/files/V. Ochrona ppoż..doc", courseDyd);
        createCourseFileIfNotFound("AED + BHP2.docx", "/files/AED + BHP2.docx", courseDyd);
        createCourseFileIfNotFound("Ma. Charakterystyczne wypadki w PŚk..docx", "/files/Ma. Charakterystyczne wypadki w PŚk..docx", courseDyd);
        createCourseFileIfNotFound("System udzielania I pomocy w PŚk.docx", "/files/System udzielania I pomocy w PŚk.docx", courseDyd);

        // Pliki dla kursu administracyjno-biurowego
        createCourseFileIfNotFound("I. Regulacje prawne.doc", "/files/Mat. szkol. admini. biurowi/I. Regulacje prawne.doc", courseAdm);
        createCourseFileIfNotFound("II. Ocena zagrożeń.doc", "/files/Mat. szkol. admini. biurowi/II. Ocena zagrożeń.doc", courseAdm);
        createCourseFileIfNotFound("III. Organ. stanowisk pracy.doc", "/files/Mat. szkol. admini. biurowi/III. Organ. stanowisk pracy.doc", courseAdm);
        createCourseFileIfNotFound("IV. Wypadki, choroby zaw..doc", "/files/Mat. szkol. admini. biurowi/IV. Wypadki, choroby zaw..doc", courseAdm);
        createCourseFileIfNotFound("V. Ochrona ppoż..doc", "/files/Mat. szkol. admini. biurowi/V. Ochrona ppoż..doc", courseAdm);
        createCourseFileIfNotFound("AED + BHP2.docx", "/files/Mat. szkol. admini. biurowi/AED + BHP2.docx", courseAdm);
        createCourseFileIfNotFound("Ma. Charakterystyczne wypadki w PŚk..docx", "/files/Mat. szkol. admini. biurowi/IVa. Charakterystyczne wypadki w PŚk..docx", courseAdm);
        createCourseFileIfNotFound("System udzielania I pomocy w PŚk.docx", "/files/Mat. szkol. admini. biurowi/System udzielania I pomocy w PŚk.docx", courseAdm);
    }

    /**
     * Inicjalizuje quizy wraz z pytaniami, jeśli nie istnieją dla danych kursów.
     */
    private void initQuizzes() {
        Course courseAdm = courseRepository.findByAccessKey(BHP_ADM_ACCESS_KEY) //
                .orElseThrow(() -> new RuntimeException("Course with key " + BHP_ADM_ACCESS_KEY + " not found. Cannot add quiz."));
        Course courseDyd = courseRepository.findByAccessKey(BHP_DYD_ACCESS_KEY) //
                .orElseThrow(() -> new RuntimeException("Course with key " + BHP_DYD_ACCESS_KEY + " not found. Cannot add quiz."));

        // --- Quiz dla kursu dydaktycznego ---
        // Adaptacja: Sprawdzenie istnienia przez pobranie quizów dla kursu i filtrowanie w pamięci.
        List<Quiz> quizzesForDydCourse = quizRepository.findByCourseId(courseDyd.getId()); //
        boolean quizDydExists = quizzesForDydCourse.stream()
                .anyMatch(q -> "Test końcowy BHP".equals(q.getTitle()));

        if (!quizDydExists) {
            Quiz quizDyd = new Quiz("Test końcowy BHP", "Sprawdzenie wiedzy ze szkolenia BHP", courseDyd, 20);
            quizRepository.save(quizDyd);

            List<QuizQuestion> questions = List.of(
                    new QuizQuestion("Do gaszenia urządzeń elektrycznych pod napięciem należy stosować:", "multiple_choice", Map.of("A", "Hydrant wewnętrzny", "B", "Gaśnicę proszkową", "C", "Gaśnice płynową"), "B", quizDyd),
                    new QuizQuestion("Pożary zaliczone do grupy B to:", "multiple_choice", Map.of("A", "Pożary ciał stałych", "B", "Pożary cieczy", "C", "Pożary gazów"), "B", quizDyd),
                    new QuizQuestion("Znak „wyjście ewakuacyjne” powinien być umieszczony:", "multiple_choice", Map.of("A", "Na każdych drzwiach wyjściowych", "B", "Na drzwiach wyjściowych z sal wykładowych", "C", "Na drzwiach przy wyjściu z budynku"), "C", quizDyd),
                    new QuizQuestion("Natężenie oświetlenia w pomieszczeniach gdzie są stanowiska pracy z monitorami ekranowymi wynosi:", "multiple_choice", Map.of("A", "200 lx", "B", "300 lx", "C", "500 lx"), "B", quizDyd),
                    new QuizQuestion("Temperatura na stanowisku biurowym – przy komputerze powinna wynosić nie mniej niż:", "multiple_choice", Map.of("A", "14oC", "B", "18oC", "C", "24oC"), "B", quizDyd),
                    new QuizQuestion("Rodzajem wentylacji miejscowej mechanicznej jest:", "multiple_choice", Map.of("A", "Dygestorium", "B", "Infiltracja", "C", "Aeracja"), "A", quizDyd),
                    new QuizQuestion("Wysokość pomieszczenia stałej pracy nie może być mniejsza niż:", "multiple_choice", Map.of("A", "3,3 m ...", "B", "3 m ...", "C", "2,5 m ..."), "B", quizDyd),
                    new QuizQuestion("Monitor komputera powinien być tak ustawiony, aby:", "multiple_choice", Map.of("A", "Środek ekranu ...", "B", "Górna krawędź ekranu ...", "C", "Środek ekranu powyżej oczu ..."), "B", quizDyd),
                    new QuizQuestion("Jedną z kar dyscyplinarnych nauczyciela akademickiego jest:", "multiple_choice", Map.of("A", "Upomnienie z wpisem do akt", "B", "Grzywna lub mandat", "C", "Pozbawienie prawa ..."), "A", quizDyd),
                    new QuizQuestion("Osoba odpowiedzialna za BHP podlega karze:", "multiple_choice", Map.of("A", "Upomnienia", "B", "Nagany", "C", "Grzywny"), "C", quizDyd),
                    new QuizQuestion("Wypadkiem przy pracy jest:", "multiple_choice", Map.of("A", "Negatywny wpływ środowiska ...", "B", "Nagłe zdarzenie ...", "C", "Każdy uraz na terenie uczelni"), "B", quizDyd),
                    new QuizQuestion("Wypadek na konferencji zagranicznej to:", "multiple_choice", Map.of("A", "Wypadek przy pracy", "B", "Szczególne okoliczności", "C", "Traktowany na równi z wypadkiem przy pracy"), "C", quizDyd),
                    new QuizQuestion("Podstawowym dokumentem do świadczeń powypadkowych:", "multiple_choice", Map.of("A", "Karta wypadku", "B", "Protokół powypadkowy", "C", "Zaświadczenie lekarskie"), "B", quizDyd),
                    new QuizQuestion("Prowadzący zajęcia w laboratorium musi:", "multiple_choice", Map.of("A", "Zgłosić zagrożenie i pracować", "B", "Zaznajomić uczestników z BHP", "C", "Stosować instrukcję tylko na swoim stanowisku"), "B", quizDyd),
                    new QuizQuestion("Wstęp do laboratorium z innej jednostki:", "multiple_choice", Map.of("A", "Za zgodą kierownika labu", "B", "Za zgodą kierownika jednostki", "C", "Za zgodą BHP lub PIP"), "B", quizDyd),
                    new QuizQuestion("Czynnik niebezpieczny w środowisku pracy:", "multiple_choice", Map.of("A", "Prąd elektryczny", "B", "Pył azbestu", "C", "Wibracja"), "A", quizDyd),
                    new QuizQuestion("Prace szczególnie niebezpieczne wymagają zgody gdy:", "multiple_choice", Map.of("A", "Wysokość >1m", "B", "Spawanie w pomieszczeniu", "C", "Obie odpowiedzi prawidłowe"), "C", quizDyd),
                    new QuizQuestion("Pierwsza pomoc przy ataku padaczki:", "multiple_choice", Map.of("A", "Leży na plecach – chronić głowę", "B", "Leży na brzuchu – przekładamy na plecy", "C", "Obie odpowiedzi prawidłowe"), "A", quizDyd),
                    new QuizQuestion("Zadławienie osoby dorosłej – co najpierw:", "multiple_choice", Map.of("A", "Wyjęcie ciała i 5 uderzeń", "B", "Wyjęcie i 5 uciśnięć brzucha", "C", "Obie odpowiedzi"), "A", quizDyd),
                    new QuizQuestion("Resuscytacja osoby porażonej prądem:", "multiple_choice", Map.of("A", "5 wdechów, 30 uciśnięć", "B", "30 uciśnięć, 2 wdechy", "C", "Nie ma to znaczenia"), "B", quizDyd)
            );
            quizQuestionRepository.saveAll(questions);
        }

        // --- Quiz dla kursu administracyjno-biurowego ---
        // Adaptacja: Sprawdzenie istnienia przez pobranie quizów dla kursu i filtrowanie w pamięci.
        List<Quiz> quizzesForAdmCourse = quizRepository.findByCourseId(courseAdm.getId()); //
        boolean quizAdmExists = quizzesForAdmCourse.stream()
                .anyMatch(q -> "Test BHP dla pracowników administracyjno-biurowych".equals(q.getTitle()));

        if (!quizAdmExists) {
            Quiz quizAdm = new Quiz("Test BHP dla pracowników administracyjno-biurowych", "Sprawdzenie wiedzy z zakresu BHP dla pracowników biurowych", courseAdm, 21);
            quizRepository.save(quizAdm);

            List<QuizQuestion> additionalQuestions = List.of(
                    new QuizQuestion("Zatrudnienie kobiet ciężarnych na stanowiskach z monitorami ekranowymi jest:", "multiple_choice", Map.of("A", "dopuszczalne do trzeciego miesiąca ciąży włącznie", "B", "niedopuszczalne", "C", "dopuszczalne w łącznym czasie nieprzekraczającym 8 godzin na dobę, przy czym czas spędzony przy obsłudze monitora ekranowego nie może jednorazowo przekraczać 50 minut, po którym to czasie powinna nastąpić co najmniej 10-minutowa przerwa, wliczana do czasu pracy"), "C", quizAdm),
                    new QuizQuestion("Najmniejsze dopuszczalne średnie natężenie oświetlenia w pomieszczeniach gdzie są stanowiska pracy z monitorami ekranowymi wynosi:", "multiple_choice", Map.of("A", "200 lx", "B", "300 lx", "C", "500 lx"), "B", quizAdm),
                    new QuizQuestion("Maksymalny dopuszczalny poziom hałasu w pomieszczeniu biurowym to:", "multiple_choice", Map.of("A", "55 dB", "B", "65 dB", "C", "85 dB"), "B", quizAdm),
                    new QuizQuestion("Do gaszenia urządzeń pod napięciem należy stosować:", "multiple_choice", Map.of("A", "gaśnicę proszkową", "B", "gaśnicę pianową", "C", "gaśnicę śniegową"), "A", quizAdm),
                    new QuizQuestion("Monitor komputera powinien być tak ustawiony, aby:", "multiple_choice", Map.of("A", "środek ekranu znajdował się na wysokości oczu operatora", "B", "górna krawędź ekranu znajdowała się na wysokości oczu operatora", "C", "środek ekranu znajdował się powyżej oczu operatora"), "B", quizAdm),
                    new QuizQuestion("Podczas pracy przy komputerze pracownik powinien sobie robić przerwy:", "multiple_choice", Map.of("A", "co najmniej 15 minut po 4 godzinach pracy", "B", "5 minut po każdej godzinie intensywnej pracy", "C", "częstotliwość przerw należy uzależnić od wskazań lekarskich"), "B", quizAdm),
                    new QuizQuestion("Udzielając pomocy osobie, która doznała ataku epilepsji:", "multiple_choice", Map.of("A", "kładziemy osobę w pozycji bocznej i wzywamy pogotowie", "B", "chronimy głowę poszkodowanego, zapewniamy dostęp świeżego powietrza, nie podajemy leków i płynów", "C", "kładziemy osobę na wznak, między zęby wkładamy twardy przedmiot i wzywamy pogotowie"), "B", quizAdm),
                    new QuizQuestion("Udzielając pomocy osobie, która się zadławiła należy zacząć od:", "multiple_choice", Map.of("A", "próby wyjęcia ciała obcego, a następnie wykonać od 5-ciu uderzeń w plecy między łopatki", "B", "próby wyjęcia ciała obcego oraz wykonać od 5-ciu uciśnięć okolicy nadbrzusza", "C", "obie odpowiedzi są prawidłowe"), "A", quizAdm),
                    new QuizQuestion("Celem okresowych badań lekarskich jest:", "multiple_choice", Map.of("A", "ustalenie, czy pod wpływem warunków pracy zachodzą w stanie zdrowia pracownika, niekorzystne zmiany", "B", "ustalenie, czy pracownik jest całkowicie zdrowy", "C", "ustalenie, czy zdolny jest do pracy po chorobie"), "A", quizAdm),
                    new QuizQuestion("Kontrolnym badaniom lekarskim poddawany jest pracownik, który:", "multiple_choice", Map.of("A", "chorował dłużej niż 30 dni", "B", "był nieobecny w pracy powyżej 30 dni", "C", "uległ wypadkowi przy pracy"), "A", quizAdm),
                    new QuizQuestion("W razie konieczności ewakuacji pracownik ma obowiązek:", "multiple_choice", Map.of("A", "biegiem opuścić budynek", "B", "wsiąść do windy i jak najszybciej zjechać na dół, po czym opuścić budynek", "C", "bez paniki wyjść z budynku klatką schodową i napotkane osoby informować o konieczności ewakuacji"), "C", quizAdm),
                    new QuizQuestion("Do czynników uciążliwych na stanowisku monitora ekranowego należą:", "multiple_choice", Map.of("A", "obciążenie układu mięśniowo-szkieletowego, obciążenie wzroku i obciążenie psychiczne", "B", "obciążenie układu mięśniowo-szkieletowego i obciążenie wzroku", "C", "obciążenie wzroku"), "A", quizAdm),
                    new QuizQuestion("Powierzchnia użytkowa przysługująca jednemu pracownikowi wynosi:", "multiple_choice", Map.of("A", "13m3 objętości pomieszczenia i 2m2 podłogi", "B", "15m3", "C", "12m2"), "A", quizAdm),
                    new QuizQuestion("W razie porażenia prądem współpracownika należy:", "multiple_choice", Map.of("A", "położyć osobę w pozycji bocznej i wzywać pogotowie", "B", "nie dotykać skóry poszkodowanego, jeśli nie ma on kontaktu z napięciem oraz nie podawać leków i płynów", "C", "odciąć dopływ prądu (bezpiecznik lub wtyczka z kontaktu) lub odsunąć poszkodowanego od źródła prądu za pomocą drewnianego przedmiotu, stojąc na suchej macie gumowej, książce lub złożonej gazecie"), "C", quizAdm),
                    new QuizQuestion("Pracownik za nie przestrzeganie przepisów i zasad bezpieczeństwa i higieny pracy podlega karze:", "multiple_choice", Map.of("A", "pieniężnej", "B", "pozbawienia wolności", "C", "zwolnienia dyscyplinarnego"), "A", quizAdm),
                    new QuizQuestion("Gdy pracownik administracyjny uległ wypadkowi w czasie podróży służbowej, to uległ:", "multiple_choice", Map.of("A", "wypadkowi przy pracy", "B", "wypadkowi traktowanemu na równi z wypadkiem przy pracy", "C", "wypadkowi w okresie ubezpieczenia"), "B", quizAdm),
                    new QuizQuestion("Za stan bezpieczeństwa i higieny pracy w zakładzie odpowiedzialność ponosi:", "multiple_choice", Map.of("A", "Służba BHP", "B", "Pracodawca", "C", "Państwowa Inspekcja Pracy"), "B", quizAdm),
                    new QuizQuestion("Pożary grupy B, to pożary:", "multiple_choice", Map.of("A", "ciał stałych", "B", "tłuszczy kuchennych", "C", "cieczy palnych"), "C", quizAdm),
                    new QuizQuestion("Pracodawca jest zobowiązany zapewnić pracownikom napoje chłodzące gdy temperatura w pomieszczeniu biurowym przekracza:", "multiple_choice", Map.of("A", "25oC", "B", "28oC", "C", "30oC"), "B", quizAdm),
                    new QuizQuestion("Wypadkiem przy pracy jest:", "multiple_choice", Map.of("A", "negatywny wpływ środowiska pracy na organizm ludzki", "B", "nagłe zdarzenie, wywołane przyczyną zewnątrzną, mające związek z pracą i powodujące uraz lub śmierć", "C", "każdy uraz doznany przez pracownika na terenie uczelni"), "B", quizAdm),
                    new QuizQuestion("Podstawowym dokumentem uprawniającym do świadczeń z tytułu wypadku przy pracy jest:", "multiple_choice", Map.of("A", "karta wypadku przy pracy", "B", "protokół powypadkowy", "C", "zaświadczenie lekarskie o niezdolności do pracy z tytułu wypadku"), "B", quizAdm)
            );
            quizQuestionRepository.saveAll(additionalQuestions);
        }
    }

    // --- Metody Pomocnicze ---

    /**
     * Tworzy i zapisuje użytkownika, jeśli żaden o podanej nazwie nie istnieje.
     */
    private void createUserIfNotFound(String username, String password, UserRole role) {
        if (userRepository.findByUsername(username).isEmpty()) { //
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            userRepository.save(user);
        }
    }

    /**
     * Tworzy i zapisuje plik kursu, jeśli plik o tej samej ścieżce nie jest jeszcze przypisany do kursu.
     * Adaptacja: Sprawdza istnienie przez pobranie listy i filtrowanie w pamięci.
     */
    private void createCourseFileIfNotFound(String fileName, String filePath, Course course) {
        List<CourseFile> filesForCourse = courseFileRepository.findByCourseId(course.getId()); //
        boolean fileExists = filesForCourse.stream()
                .anyMatch(file -> filePath.equals(file.getFileUrl()));

        if (!fileExists) {
            CourseFile courseFile = new CourseFile(fileName, filePath, course);
            courseFileRepository.save(courseFile);
        }
    }
}