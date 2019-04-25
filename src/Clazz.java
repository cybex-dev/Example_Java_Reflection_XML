import java.util.ArrayList;
import java.util.List;

public class Clazz {
    private String id;
    private String room;
    private List<Student> studentList = new ArrayList<>();

    public Clazz(String id, String room) {
        this.id = id;
        this.room = room;
    }

    public String getId() {
        return id;
    }

    public String getRoom() {
        return room;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void addStudent(Student student) {
        studentList.add(student);
    }
}
