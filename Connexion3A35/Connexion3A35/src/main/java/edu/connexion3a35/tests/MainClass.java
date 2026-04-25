package edu.connexion3a35.tests;

import edu.connexion3a35.entities.Personne;
import edu.connexion3a35.services.PersonneService;
import edu.connexion3a35.tools.MyConnection;

public class MainClass {

    public static void main(String[] args) {
        //MyConnection mc = new MyConnection();
        /*
        Personne p = new Personne("Tyson","Mike");
        PersonneService ps = new PersonneService();
        ps.addEntity2(p);

         */
        MyConnection mc1 = MyConnection.getInstance();
        MyConnection mc2 = MyConnection.getInstance();
        System.out.println(mc1.hashCode()+" - "+mc2.hashCode());
    }
}
