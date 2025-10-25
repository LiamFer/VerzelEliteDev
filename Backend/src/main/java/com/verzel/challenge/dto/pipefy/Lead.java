package com.verzel.challenge.dto.pipefy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Lead {
    private String nome;
    private String email;
    private String empresa;
    private String necessidade;
    private Boolean interesse;

    @Override
    public String toString() {
        return "Lead{" +
                "nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", empresa='" + empresa + '\'' +
                ", necessidade='" + necessidade + '\'' +
                ", interesse=" + interesse +
                '}';
    }
}