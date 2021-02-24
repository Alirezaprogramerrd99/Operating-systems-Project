

import java.util.concurrent.Semaphore;


/*  Alireza-Rashidi        96243097         problem solved for (n == 1)  */


class SharedData {

    static Semaphore patientsSem = new Semaphore(0);
    static Semaphore doctorsSem = new Semaphore(0);
    static Semaphore roomSem = new Semaphore(0);
    static Semaphore mutex = new Semaphore(1);    // this Semaphore is for applying mutual exclusion Condition
    static int patientsCount = 0;
    static int roomCapacity = 10;            // its m

}

class Doctor extends Thread {

    private String doctorName;
    int id;

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

                System.out.println("doctor is waiting ...");
                SharedData.patientsSem.acquire();
                // System.out.println("doctor found patient");

                SharedData.mutex.acquire();
                SharedData.patientsCount--;

                SharedData.doctorsSem.release();   // from this line one patient is going to doctor.
                System.out.println("doctor has signaled for new patient");
                SharedData.mutex.release();

                done();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void done() throws InterruptedException {

        SharedData.roomSem.acquire();     // doctor must wait until the patient's work done
    }

}

class Patient extends Thread {

    static long startTime;
    private String name;
    private int time;
    private long endTime;   // end time for each process.
    private int totalTime;

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
        //System.out.println(sec + " seconds");
        return sec;
    }

    @Override
    public void run() {

        try {

            Thread.sleep(this.time * 1000);
            System.out.println("process " + this.name + " created!");
            System.out.println(this.name + " 's entrance time: " + Math.round(patientElapsedTime()));

            SharedData.mutex.acquire();
            System.out.println(this.name + " enterd");
            //----------------------------------------- critical section -------------------------------------------
            if (SharedData.patientsCount < SharedData.roomCapacity) {

                SharedData.patientsCount++;
                SharedData.patientsSem.release();    // one patient is available
                SharedData.mutex.release();         // release the lock for an other patients to come.

                //----------------------------------------- critical section ---------------------------------------
                SharedData.doctorsSem.acquire();   // patient must wait until doctor signals doctorSem. until doctor permits.
                System.out.println(this.name + " visited by doctor.");
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

        Thread.sleep(2000);
        this.totalTime = Math.round(patientElapsedTime());
        //** if patient's works has been done, we signal this semaphore to release the doctor that serviced current patient
        SharedData.roomSem.release();
    }

}

public class PatientsDoctorsProblemN1 {

    private static float getElapsedTime(long start, long end) {

        float sec = (end - start) / 1000F;
        //System.out.println(sec + " seconds");
        return sec;
    }

    static void createPatients(Patient[] patients) throws InterruptedException {

        for (int i = 0; i < patients.length; i++)
            patients[i].start();

        for (int i = 0; i < patients.length; i++)
            patients[i].join();
    }

    public static void main(String[] args) {

        Patient.startTime = System.currentTimeMillis();  // base time starts from here


        Patient[] patients = new Patient[]{new Patient("P1", 1),
                new Patient("P2", 3), new Patient("P3", 5), new Patient("P4", 1)};

        Doctor doctor = new Doctor();
        doctor.start();

        try {       // exception handling block

            createPatients(patients);
            doctor.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //System.out.println("main");
    }
}


