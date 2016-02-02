import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by vadim on 03.02.16.
 */
public class Board {

    Chip cpu;
    Chip ram;

    public Board() {
        cpu = new Chip();
        ram = new Chip();
    }

    public void getPins() {
        try {
            FileInputStream is = new FileInputStream("data/coordinates.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while((line = reader.readLine()) != null)
            {
                String[] ar = line.split(";");

                int pin_num = Integer.parseInt(ar[0]) - 1;
                int chip_num = Integer.parseInt(ar[1]);
                double x = Double.parseDouble(ar[2].replace(',', '.'));
                double y = Double.parseDouble(ar[3].replace(',', '.'));

                if (chip_num == 1)
                    cpu.pins[pin_num] = new Pin(x, y);
                else
                    ram.pins[pin_num] = new Pin(x, y);
            }
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    public void getConnections() {
        try {
            FileInputStream is = new FileInputStream("data/connect.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while((line = reader.readLine()) != null)
            {
                String[] ar = line.split(";");

                int cpu_pin = Integer.parseInt(ar[0]) - 1;
                int ram_pin = Integer.parseInt(ar[1]) - 1;

                cpu.connections[cpu_pin] = ram_pin;
                ram.connections[ram_pin] = cpu_pin;
            }
        } catch (IOException x) {
            System.err.println(x);
        }
    }
}
