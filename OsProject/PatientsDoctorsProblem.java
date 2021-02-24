
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/*  Alireza-Rashidi        96243097  */


class SharedData {

    static int doctorsCount = 4;                            // it is n the number of doctors.
    static Semaphore patientsSem = new Semaphore(0);
    static Semaphore[] doctorsSem = new Semaphore[doctorsCount];
    static Semaphore[] roomSem = new Semaphore[doctorsCount];   // for each doctors room we have semaphore.
    static Semaphore mutex = new Semaphore(1);    // this Semaphore is for applying mutual exclusion Condition
    static Semaphore availableDoctorsSem = new Semaphore(doctorsCount);    // at the initial time all doctors are available.
    static int patientsCount = 0;
    static int roomCapacity = 10;            // its waiting room capacity.

    static {
        for (int i = 0; i < doctorsCount; i++) {
            doctorsSem[i] = new Semaphore(0);
            roomSem[i] = new Semaphore(0);
        }
    }

}

class Doctor extends Thread {

    private String doctorName;
    private int id;             // with help of this field we map each doctor to its room and its semaphore.
    static ArrayList<Doctor> availableDoctors = new ArrayList<>();
    Patient doctorsPatient;

    Doctor() {
    }

    Doctor(String doctorName, int id) {

        super(doctorName);
        this.doctorName = doctorName;
        this.id = id;
    }

    public void run() {    // doctor thread code.

        while (true) {

            try {

                System.out.println("the doctor " + this.doctorName + " is waiting ...");
                SharedData.patientsSem.acquire();
                //System.out.println("doctor found patient");

                SharedData.mutex.acquire();
                SharedData.patientsCount--;

                SharedData.doctorsSem[this.id].release();   // from this line one patient is going to doctor.
                System.out.println("doctor " + this.doctorName + " has signaled for new patient");
                SharedData.mutex.release();

                done();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void done() throws InterruptedException {

        SharedData.roomSem[this.id].acquire();     // doctor must wait until the patient's work done

    }

    String getDoctorName() {
        return this.doctorName;
    }

    public int getDoctorID() {
        return this.id;
    }
}

class Patient extends Thread {

    static long startTime;
    private String name;
    private int time;
    private long endTime;   // end time for each process.
    private int totalTime;
    private Doctor patientDoctor;

    Patient(String name, int time) {
        super(name);
        this.name = name;
        this.time = time;
        this.totalTime = 0;
    }

    public String getPName() {
        return name;
    }

    public int getEntryTime() {
        return time;
    }

    public float patientElapsedTime() {

        this.endTime = System.currentTimeMillis();
        float sec = (this.endTime - startTime) / 1000F;
        return sec;
    }

    @Override
    public void run() {

        try {

            Thread.sleep(this.time * 1000);
            System.out.println("process " + this.name + " created!");
            System.out.println(this.name + " 's entrance time: " + Math.round(patientElapsedTime()));

            SharedData.mutex.acquire();
            System.out.println(this.name + " entered");
            //----------------------------------------- critical section -------------------------------------------
            if (SharedData.patientsCount < SharedData.roomCapacity) {

                SharedData.patientsCount++;

                SharedData.patientsSem.release();    // one patient is available
                SharedData.mutex.release();         // release the lock for an other patients to come.

                SharedData.availableDoctorsSem.acquire();
                this.patientDoctor = Doctor.availableDoctors.remove(0);
                this.patientDoctor.doctorsPatient = this;
                // System.out.println(patientDoctor.getDoctorID());
                SharedData.doctorsSem[patientDoctor.getDoctorID()].acquire();   // patient must wait until doctor signals doctorSem. until doctor permits.
                System.out.println(this.name + " visited by doctor " + patientDoctor.getDoctorName() + "\n");
                getIn();

                System.out.println(this.name + " finished and elapsed time is: " + this.totalTime + "\n");

            } else {
                SharedData.mutex.release();
            }

        } catch (InterruptedException exc) {
            exc.printStackTrace();
        }
    }

    private void getIn() throws InterruptedException {

        Doctor.availableDoctors.add(this.patientDoctor);

        Thread.sleep(2000);
        this.totalTime = Math.round(patientElapsedTime());

        SharedData.availableDoctorsSem.release();
        System.out.println(this.name + " released doctor " + this.patientDoctor.getName());
        SharedData.roomSem[patientDoctor.getDoctorID()].release();

        //** if patient's works has been done, we signal this semaphore to release the doctor that serviced current patient
        // the doctor is available again.

        System.out.println("visit " + this.name + " finished!");
    }

}

public class PatientsDoctorsProblem {

    static void createPatients(Patient[] patients) throws InterruptedException {

        for (int i = 0; i < patients.length; i++)
            patients[i].start();

        for (int i = 0; i < patients.length; i++)
            patients[i].join();
    }

    static void setDoctors(Doctor[] doctors) throws InterruptedException {

        for (int i = 0; i < doctors.length; i++) {
            Doctor.availableDoctors.add(doctors[i]);

        }

        for (Doctor doctor : doctors) {
            doctor.start();             // starting the doctor's thread.
        }

    }


    public static void main(String[] args) {

        Patient.startTime = System.currentTimeMillis();  // base time starts from here

        Patient[] patients = new Patient[]{new Patient("P1", 1),
                new Patient("P2", 3), new Patient("P3", 6), new Patient("P4", 1), new Patient("P5", 2)};

        // Doctor doctor = new Doctor();
        // doctor.start();

        Doctor[] doctors = new Doctor[]{new Doctor("D1", 0), new Doctor("D2", 1), new Doctor("D3", 2), new Doctor("D4", 3)};
        // Doctor[] doctors = new Doctor[]{new Doctor("D1", 0)};

        try {       // exception handling block

            setDoctors(doctors);
            createPatients(patients);

            for (Doctor doctor : doctors) {
                doctor.join();
            }
            // doctor.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //System.out.println("main");
    }
}
