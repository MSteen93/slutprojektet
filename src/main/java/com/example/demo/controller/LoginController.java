package com.example.demo.controller;


import com.example.demo.model.Plant;
import com.example.demo.model.PlantLog;
import com.example.demo.repository.PlantLogRepository;
import com.example.demo.repository.PlantRepository;
import com.example.demo.repository.SpeciesRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.LoginService;
import com.example.demo.model.Admin;
import com.example.demo.service.PlantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
public class LoginController {
    @Autowired
    LoginService loginService;
    @Autowired
    PlantRepository plantRepository;
    @Autowired
    SpeciesRepository speciesRepository;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PlantService plantService;
    @Autowired
    PlantLogRepository plantLogRepository;


    @GetMapping("/")
    public String LoadLandingPage(Model model, HttpSession session) {
        model.addAttribute("admin", new Admin());
        return "login";
    }

    @PostMapping("/createUser")
    public RedirectView postRegistration(@ModelAttribute Admin admin, RedirectAttributes redir) {
        RedirectView rvLogin = new RedirectView("/", true);
        //If user already exists.. Model.addAttribute (first Validation exercise)
        admin.setPassword(encoder.encode(admin.getPassword()));
        loginService.addUser(admin);
        redir.addFlashAttribute("NewAccountSuccess", "Your registration is confirmed.");
             return rvLogin;
    }

    @PostMapping("/")
    public String postLogin(Model model, HttpSession session, @RequestParam String email,
                            @RequestParam String password) {
        for (Admin admin : loginService.getUsers()) {
            if (email.equals(admin.getEmail())) {
                session.setAttribute("userId", admin.getId());
            }
        }
        return "home";
    }

    @GetMapping("/home")
    public String LoadHomePage(Model model) {
        Admin admin = getLoggedInAdmin();
        List<Plant> userPlants = plantRepository.findAllByAdminId(admin.getId());
        model.addAttribute("admin", admin);
        model.addAttribute("plants", userPlants);
        model.addAttribute("userId", admin.getId());
        model.addAttribute("plant", new Plant());
        return "home";
    }


    //Home to Plantdescription
    @GetMapping("/plant/{id}")
    public String PlantDescription(Model model, @PathVariable Long id) {
        Admin admin = getLoggedInAdmin();
        List<Plant> userPlants = plantRepository.findAllByAdminId(admin.getId());
        Plant plant = plantRepository.findById(id).get();
        if (userPlants.contains(plant)) {
            model.addAttribute("eventDayValue", plantService.eventDayValue(id));
            model.addAttribute("plant", plant);
            model.addAttribute("admin", admin);
            model.addAttribute("timeline", plantService.nextFiveTimeline(id));
            return "plantdescription";
        }
        else {
            return "redirect:/home";
        }
    }

    @GetMapping("/createPlantLog/{event}/{id}")
    public String plantLog(@PathVariable String event, @PathVariable Long id) {
        PlantLog plantLog = new PlantLog();
        plantLog.setPlant(plantRepository.findById(id).get());
        plantLog.setEvent(event);
        plantLogRepository.save(plantLog);
        System.out.println(plantService.todaysTimeline(1L));
        return "redirect:/plant/" + id;
    }

    @PostMapping("/save")
    public String savePlant(@ModelAttribute Plant plant, HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        model.addAttribute("userId", userId);
        plantRepository.save(plant);
        ra.addFlashAttribute("SuccesPlantCreation", "Your plant has been added to your collection.");
        return "redirect:/home";
    }


    @GetMapping("/logout")
    public String logout() {
        return "login";
    }

    private Admin getLoggedInAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        Admin admin = userRepository.findByEmail(currentPrincipalName);
        return admin;
    }
    //Delete Plant
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id,RedirectAttributes ra) throws Exception {
        Admin admin = getLoggedInAdmin();
        List<Plant> userPlants = plantRepository.findAllByAdminId(admin.getId());
        Plant plant = plantRepository.findById(id).get();
        if (userPlants.contains(plant)) {
            plantService.deletePlant(id);
            ra.addFlashAttribute("SuccesPlantCreation", "Your plant has been removed");
            return "redirect:/home";
        } else {
            ra.addFlashAttribute("ErrorPlantRemoval", "No such plant");
            return "redirect:/home";
        }

    }

}