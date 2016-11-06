package com.ote.app;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Application {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Application app = new Application();

       /* List<Integer> list = new ArrayList<>();

        for (int i =0; i<20; i++){
            list.add(i);
        }

        app.getPage(list, 6);*/

        System.out.println(app.getPage(4, 5));
        System.out.println(app.getPage(6, 8));
        System.out.println(app.getPage(8, 6));
        System.out.println(app.getPage(13, 12));

    }

    public List<Integer> getPage(List<Integer> list, int pageNumber) {

        return null;
    }

    public int getPage(int pageNumber, int pageSize) {

        return pageNumber / pageSize;
    }
}
