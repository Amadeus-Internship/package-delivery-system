package com.example.package_delivery_system.services.impl;

import com.example.package_delivery_system.data.dtos.addressDtos.AddressDto;
import com.example.package_delivery_system.data.dtos.user.UserRegisterDto;
import com.example.package_delivery_system.data.dtos.user.UserResponseDto;
import com.example.package_delivery_system.data.entities.Role;
import com.example.package_delivery_system.data.entities.UserEntity;
import com.example.package_delivery_system.data.repositories.RoleRepository;
import com.example.package_delivery_system.data.repositories.UserRepository;
import com.example.package_delivery_system.exceptions.UserExceptions;
import com.example.package_delivery_system.services.AddressService;
import com.example.package_delivery_system.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AddressService addressService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           RoleRepository roleRepository, AddressService addressService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.addressService = addressService;
    }

    //make method return a response dto
    @Override
    @Transactional
    public UserResponseDto register(UserRegisterDto userRegisterDto) {

        if (this.roleRepository.count() == 0) {
            this.seedRoles();
        }
        String username = userRegisterDto.getUsername();
        String fullName = userRegisterDto.getFirstName() + " " + userRegisterDto.getLastName();
        String UCN = userRegisterDto.getUCN();
        String phone = userRegisterDto.getPhone();
        String eMail = userRegisterDto.getEmail();
        String password = userRegisterDto.getPassword();
        String confirmPassword = userRegisterDto.getConfirmPassword();

        if (this.userRepository.existsByUsernameOrEmail(username, eMail)) {
            System.out.printf(UserExceptions.USER_ALREADY_EXISTS, username);
        }
        if (!password.equals(confirmPassword)) {
            System.out.println(UserExceptions.PASSWORDS_DO_NOT_MATCH);
        }
        if (this.userRepository.existsByPhone(phone)) {
            System.out.println(UserExceptions.PHONE_ALREADY_EXISTS);
        }
        AddressDto userAddress = this.addressService.createUserAddress(userRegisterDto);

        UserEntity user = new UserEntity();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPhone(phone);
        user.setUCN(UCN);
        user.setAddressId(((AddressServiceImpl) this.addressService).findById(userAddress.getId()));
        user.setEmail(eMail);
        user.setPassword(passwordEncoder.encode(password));
        if (this.userRepository.count() == 0) {
            user.setRoles(Set.of(this.roleRepository.getRoleByAuthority("ADMIN").get()));
        } else {
            user.setRoles(Set.of(this.roleRepository.getRoleByAuthority("CUSTOMER").get()));
        }
        this.userRepository.save(user);


        UserResponseDto userResponseDto = new UserResponseDto(user.getId(), username, fullName, UCN, phone, eMail);
        return userResponseDto;
    }

    @Override
    public void seedRoles() {
        Role adminRole = new Role("ADMIN");
        Role customerRole = new Role("CUSTOMER");
        Role agentRole = new Role("AGENT");
        Role driverRole = new Role("DRIVER");

        Set<Role> roles = new HashSet<>();

        roles.add(adminRole);
        roles.add(customerRole);
        roles.add(agentRole);
        roles.add(driverRole);

        this.roleRepository.saveAll(roles);
    }

    // implemented by UserDetailsService
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        final UserEntity user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(email);
        }
        return user;
    }
}
