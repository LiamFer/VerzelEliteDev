package com.verzel.challenge.mapper;

import com.verzel.challenge.dto.pipefy.Card;
import com.verzel.challenge.dto.pipefy.Lead;
import com.verzel.challenge.entity.LeadEntity;

public class LeadMapper {
    public static Lead toLead(Card card) {
        Lead lead = new Lead();
        if (card.fields == null) return lead;
        for (Card.Field f : card.fields) {
            if (f.name == null) continue;
            switch (f.name.trim().toLowerCase()) {
                case "nome":
                    lead.setNome(f.value);
                    break;
                case "e-mail":
                    lead.setEmail(f.value);
                    break;
                case "empresa":
                    lead.setEmpresa(f.value);
                    break;
                case "necessidade":
                    lead.setNecessidade(f.value);
                    break;
                case "interessado":
                    Boolean interesse = null;
                    if(f.value.equalsIgnoreCase("sim")){
                        interesse = true;
                    } else if (f.value.equalsIgnoreCase("não")){
                        interesse = false;
                    }
                    lead.setInteresse(interesse);
                    break;
            }
        }
        return lead;
    }

    public static Lead toLead(LeadEntity leadEntity){
        if(leadEntity == null) return new Lead();
        return new Lead(leadEntity.getName(), leadEntity.getEmail(), leadEntity.getCompany(), leadEntity.getNecessity(), leadEntity.getInterested());
    }
}