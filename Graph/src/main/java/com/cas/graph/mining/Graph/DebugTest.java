package com.cas.graph.mining.Graph;

public class DebugTest {

 

     public static void main(String[] args) {

          fun1();

     }

 

     private static void fun1() {

          System.out.println("fun1 called");

          fun2();

     }

 

     private static void fun2() {

           System.out.println("fun2 called");

          fun3();

     }

      

     private static void fun3() {

          System.out.println("fun3 called");
          int m = 1;

          int n = 2;

          {

               int k = 3;

               int j = 4;

               m = k + j;

               n = k - j;

          }

          int t = m + n;

     }

}