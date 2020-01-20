package com.zs.seckill;

import org.springframework.context.annotation.Bean;

public class Test {


    public static void main(String[] args) {
    }

    public static void aMethod(String[] arguments) {
        System.out.println(arguments);
        System.out.println(arguments[1]);
    }

}

class Base {
    private void method1(int iBase) {
        System.out.println("Base.method1");
    }
}

class Over extends Base {
    public static void main(String[] args) {
        Over over = new Over();
        int iBase = 0;
        over.method1(iBase);
    }

    public void method1(int iOver) {
        System.out.println("Over.method1");
    }
}

class Test1 {
    static int i;

    public static void main(String[] args) {
        System.out.println(i);
    }
}

class Test2 {
    public static void main(String[] args) {
        int i = 9;
        switch (i) {
            default:
                System.out.println("d");
            case 0:
                System.out.println(0);
                break;
            case 1:
                System.out.println(1);
            case 2:
                System.out.println(2);
        }
    }
}
