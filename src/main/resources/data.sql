DELETE FROM LISTING;

INSERT INTO LISTING 
(CAMPUS, CATEGORY, CONDITION_LABEL, LOCATION, PRICE, STATUS, TITLE, DESCRIPTION, COVER_IMAGE, IMAGES_JSON)
VALUES
('主校区','数码','9成新','图书馆',1500.00,'active','二手 iPad Air','带原装充电器',
 '/adminlte/dist/img/photo1.png', '["/adminlte/dist/img/photo2.png","/adminlte/dist/img/photo3.jpg"]'),

('东校区','图书','8成新','宿舍园区',80.00,'active','考研资料全套','资料齐全',
 '/adminlte/dist/img/photo4.jpg', '["/adminlte/dist/img/photo1.png","/adminlte/dist/img/photo2.png"]'),

('东校区','家电','7成新','食堂旁',200.00,'active','小冰箱','制冷正常',
 '/adminlte/dist/img/photo3.jpg', '["/adminlte/dist/img/photo4.jpg","/adminlte/dist/img/photo1.png"]');
