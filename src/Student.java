public class Student {
    public int id;
    private String name = "temp";
    private int age;
    private boolean isMale;

    public Student() {}

    public Student(int id, int age, boolean isMale) {
        this.id = id;
        this.age = age;
        this.isMale = isMale;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public boolean isMale() {
        return isMale;
    }

    public void setId(int id) {
        this.id = id;
    }

    private void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setMale(boolean male) {
        isMale = male;
    }
}
