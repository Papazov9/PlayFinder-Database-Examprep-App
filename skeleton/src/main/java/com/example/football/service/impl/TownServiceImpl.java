package com.example.football.service.impl;

import com.example.football.models.dto.ImportTownDTO;
import com.example.football.models.entity.Town;
import com.example.football.repository.TownRepository;
import com.example.football.service.TownService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.dom4j.rule.Mode;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TownServiceImpl implements TownService {

    private final TownRepository townRepository;
    private final Gson gson;
    private final Validator validator;
    private final ModelMapper modelMapper;

    @Autowired
    public TownServiceImpl(TownRepository townRepository, Gson gson, Validator validator, ModelMapper modelMapper) {
        this.townRepository = townRepository;
        this.modelMapper = modelMapper;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.gson = gson;
    }

    @Override
    public boolean areImported() {
        return this.townRepository.count() > 0;
    }

    @Override
    public String readTownsFileContent() throws IOException {
        Path path = Path.of("src/main/resources/files/json/towns.json");

        return Files.readString(path);
    }

    @Override
    public String importTowns() throws IOException {
        ImportTownDTO[] importTownDTOs = this.gson.fromJson(readTownsFileContent(), ImportTownDTO[].class);
        return Arrays.stream(importTownDTOs)
                .map(this::importTown)
                .collect(Collectors.joining("\n"));
    }

    private String importTown(ImportTownDTO importTownDTO) {
        Set<ConstraintViolation<ImportTownDTO>> errors = this.validator.validate(importTownDTO);

        if (!errors.isEmpty()) {
            return "Invalid Town";
        }

        Optional<Town> optionalTown = this.townRepository.findByName(importTownDTO.getName());

        if (optionalTown.isPresent()) {
            return "Invalid Town";
        }

        Town town = this.modelMapper.map(importTownDTO, Town.class);

        this.townRepository.save(town);

        return String.format("Successfully imported Town %s - %d",town.getName(), town.getPopulation());
    }
}
