package HugeTextFileSorter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Sorter {
    private static ArrayList<String> tempFileNamesList = new ArrayList<>(); //Хранит пути временных файлов
    private static Map<Integer, String> map = new HashMap<>();  //Используется как буфер для сортировки строк при слиянии временных файлов


    public static void main(String[] args) throws IOException {
        long start = System.nanoTime();
        fileGenerator("a.txt", 500, 40000000);
        rowSorter("a.txt", 1000000, "result.txt");
        long time = System.nanoTime() - start;
        System.out.printf("Took %.3f second to read, sort and write to a file%n", time / 1e9);
    }

    public static void rowSorter(String fileName, int linesPerChunk, String resultFileName) {
        try {
            fileSplitter(fileName, linesPerChunk);
            sort(resultFileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /*
    *   Метод fileGenerator создает текстовой файл с указанными параметрами:
    *       - fileName: это полный путь до файла, включая название самого файла
    *       - lineSize: длина генерируемых строк
    *       - lines: количество строк
    *
    * Для генерации текста используется массив char'ов и класс Random */


    public static void fileGenerator(String fileName, int lineSize, int lines) throws FileNotFoundException {
        long start = System.nanoTime();
        System.out.println("Creating file to load");
        PrintWriter pw = new PrintWriter(fileName);
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        char[] charsArr = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', '1', '2', '3', '4', '5'};

        for (int i = 0; i < lines; i++) {
            while (sb.length() < lineSize) {
                int n = rand.nextInt(charsArr.length);
                sb.append(charsArr[n]);
            }
            pw.println(sb);
            sb.delete(0, sb.length() - 1);
        }
        pw.close();

        long time = System.nanoTime() - start;
        System.out.printf("File created. It took %.3f sec.\n", time / 1e9);
    }

    /*
    * Метод fileSplitter разбивает большой файл, на множество мелких временных файлов, при этом производя сортировку
    * строк в алфавитном порядке.
    * Принимает 2 параметра:
    *   - fileName: путь до файла, включая имя файла
    *   - linesPerChunk: параметр-делитель, количество строк, по которым будет происходить разбиение файла*/

    public static void fileSplitter(String fileName, int linesPerChunk) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        ArrayList<String> rowList = new ArrayList<>();
        PrintWriter pw;
        String line;
        long start = System.nanoTime();
        System.out.println("Start splitting file....");

        try {
            line = br.readLine();

            for (int i = 1; line != null ; i++) {
                String tempFileName = fileName + "_part_" + i + ".txt";
                tempFileNamesList.add(tempFileName);
                pw = new PrintWriter(new FileWriter(tempFileName));
                for (int j = 0; j < linesPerChunk && line != null; j++) {
                    rowList.add(line);
                    line = br.readLine();
                }

                Collections.sort(rowList);

                for(String s: rowList) {
                    pw.println(s);
                }
                pw.flush();
                pw.close();
                rowList.clear();
            }

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        long time = System.nanoTime() - start;
        System.out.println("Split took [sec]: " + (time / 1e9));
    }


    /*
    * Метод sort производит сортировку строк в алфавитном порядке и слияние временных файлов.
    * Из временных файлов читается по 1 строке за 1 итерацию, они записываются в Map, внутри Map'a происходит сортировка
    * и наименьшая строка записывается уже в целевой файл с резельтатом сортировки. При этом строка, которую мы записали,
    * удаляется из Map, ключ удаленной строки указывал на поток, из которого была прочитана строка, из того же потока,
    * на место удаленной строки, помещается следующая строка.
    * Таким оразом Map играет роль буффера */

    public static void sort(String resultFileName) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(resultFileName));
        List frList = new ArrayList();                            //Лист для хранения потоков чтения
        int countDone = 0;                                        //Для подсчета файлов в которых обработаны все строки
        for (String s: tempFileNamesList) {
            frList.add(new BufferedReader(new FileReader(s)));    //Для каждого времменного файла создается поток чтения и заносится в List потоков
        }

        OUTER:while (true) {

            for (int i = 0; i < frList.size(); i++) {                 //Чередование потоков чтения для работы с ними
                BufferedReader fr = (BufferedReader) frList.get(i);
                if (map.get(i) != null) {
                    continue;
                }
                 try {
                     String line = fr.readLine();
                     if (line == null) {
                         countDone++;
                         if (countDone == tempFileNamesList.size()) {
                             break OUTER;
                         }

                     } else {
                         map.put(i, line);
                     }
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
            }
            countDone = 0;
            pw.println(getLowerStringFromMap());

        }
        pw.flush();
        pw.close();
        for (Object br: frList) {
            ((BufferedReader)br).close();           //Закрытие потоков
        }

        for(String fileName: tempFileNamesList) {   //Удаление временных файлов
            Files.delete(Path.of(fileName));
        }
    }

    /*
    * Метод getLowerStringFromMap прозводит сотрировку Map'a по значению в алфавитном порядке, возвращает наименьшее
    * значение и удаляет из множества элемент с этим значением */

    private static String getLowerStringFromMap() {
        Map<Integer, String> tMap = new LinkedHashMap<>();
        Stream <Map.Entry<Integer, String>> st = map.entrySet().stream();
        st.sorted(Comparator.comparing(e-> e.getValue())).forEach(e->tMap.put(e.getKey(), e.getValue()));
        int key = tMap.entrySet().iterator().next().getKey();
        String value = tMap.get(key);
        map.remove(key);
        return value;
    }
}
