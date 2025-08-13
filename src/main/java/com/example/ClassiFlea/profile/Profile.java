// src/main/java/com/example/ClassiFlea/profile/Profile.java
package com.example.ClassiFlea.profile;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="profiles", uniqueConstraints=@UniqueConstraint(name="uk_profile_email", columnNames="userEmail"))
public class Profile {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

  @Column(nullable=false, length=120) private String userEmail; // 当前登录邮箱
  private String nickname;
  private String avatarUrl;
  private String phone;
  @Column(length=1000) private String bio;

  @Column(nullable=false) private Instant updatedAt = Instant.now();

  // getters/setters
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getUserEmail(){return userEmail;} public void setUserEmail(String v){this.userEmail=v;}
  public String getNickname(){return nickname;} public void setNickname(String v){this.nickname=v;}
  public String getAvatarUrl(){return avatarUrl;} public void setAvatarUrl(String v){this.avatarUrl=v;}
  public String getPhone(){return phone;} public void setPhone(String v){this.phone=v;}
  public String getBio(){return bio;} public void setBio(String v){this.bio=v;}
  public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant t){this.updatedAt=t;}
}
