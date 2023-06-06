package es.joseluisg.dam;

import es.joseluisg.dam.cifrador.BCrypt;

public class Main {

    public static void main(String[] args) {
        String originalPassword = "password";
        String generatedSecuredPasswordHash = BCrypt.hashpw(originalPassword, BCrypt.gensalt(12));
        System.out.println(generatedSecuredPasswordHash);

        boolean matched = BCrypt.checkpw(originalPassword, generatedSecuredPasswordHash);
        System.out.println(matched);
    }
}
