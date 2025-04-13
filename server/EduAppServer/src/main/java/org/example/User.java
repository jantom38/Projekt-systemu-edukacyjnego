package org.example;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    private String password;
    private int courseId;
    private int role;
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)

    // Gettery i settery
    public Long getId() { return id; }


    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }


    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }


    public Integer getCourseId() { return courseId; }

    public void setCourseId(Integer courseId) { this.courseId = courseId; }
}
