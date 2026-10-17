import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        System.out.println("Deadline Tracker da san sang!");

        // Dung han tuong doi de vi du van chay duoc vao ngay khac.
        LocalDateTime deadline = LocalDateTime.now().plusDays(3);

        Task firstTask = new Task(
                "Ve so do CSDL",
                "Xac dinh cac bang va moi quan he.",
                "Binh",
                deadline);

        Task secondTask = new Task(
                "Viet bao cao",
                "",
                "Chi",
                deadline.plusDays(1));

        System.out.println("\n=== Hai cong viec vua tao ===");
        printTask(firstTask);
        printTask(secondTask);

        // Moi doi tuong giu trang thai rieng cua no.
        firstTask.updateStatus(TaskStatus.IN_PROGRESS);
        System.out.println("\n=== Sau khi bat dau cong viec dau tien ===");
        System.out.println(firstTask.getTitle() + ": " + firstTask.getStatus());
        System.out.println(secondTask.getTitle() + ": " + secondTask.getStatus());

        // Thu du lieu sai va hien thi ly do bi tu choi.
        System.out.println("\n=== Thu tao cong viec thieu ten ===");
        try {
            new Task("   ", "", "Binh", deadline);
        } catch (IllegalArgumentException error) {
            System.out.println("Tu choi: " + error.getMessage());
        }
    }

    private static void printTask(Task task) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        System.out.println("Ten: " + task.getTitle());
        System.out.println("Mo ta: " + task.getDescription());
        System.out.println("Nguoi phu trach: " + task.getAssigneeName());
        System.out.println("Han nop: " + task.getDueAt().format(formatter));
        System.out.println("Trang thai: " + task.getStatus());
        System.out.println();
    }
}
