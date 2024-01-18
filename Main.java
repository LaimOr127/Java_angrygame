import java.util.ArrayList;
import java.util.List;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import matterjs.Matter;
import matterjs.Matter.Body;
import matterjs.Matter.Engine;
import matterjs.Matter.Events;
import matterjs.Matter.World;
import matterjs.Matter.Bodies;

public class Main extends PApplet {
    Engine engine;  // Создаем механизм физической симуляции
    Body ground;  // Создаем объект для земли
    Body pumpkin;  // Создаем объект для тыквы
    SlingShot slingshot;  // Создаем объект рогатки
    List<GameObject> boxes;  // Создаем список игровых объектов (ящиков)
    boolean pumpkinReleased = false;  // Флаг для отслеживания выпущенной тыквы
    boolean pumpkinHasCollided = false;  // Флаг для отслеживания столкновения тыквы с другими объектами
    boolean pumpkinBeingDragged = false;  // Флаг для отслеживания перетаскивания тыквы
    boolean gameStarted = false;  // Флаг для отслеживания начала игры
    ExplosionManager explosionManager;  // Создаем менеджер взрывов
    Torch torch1;  // Создаем факел 1
    Torch torch2;  // Создаем факел 2

    int maxStretch = 100;  // Максимальное растяжение рогатки
    float strength = 0.00161F;  // Сила броска тыквы
    float simulationSpeed = 0.8F;  // Скорость симуляции
    int interactRadius = 50;  // Радиус взаимодействия с мышью

    PImage titleScreen;
    PImage imgSkull;
    PImage pumpkinImg;
    PImage imgBoxSkull;
    PImage imgStone1;
    PImage imgBone1;
    PImage imgPlank1;
    PImage monsterImg;

    public Main() {
        boxes = new ArrayList<GameObject>();
    }

    public void setup() {
        size(1500, 800);  // Устанавливаем размер окна
        engine = Matter.Engine.create();  // Создаем механизм физической симуляции
        engine.timing.timeScale = simulationSpeed;  // Устанавливаем скорость симуляции

        ground = Bodies.rectangle(width / 2, height - 100, width, 20);  // Создаем землю
        World.add(engine.world, ground);  // Добавляем землю в мир

        pumpkin = Bodies.circle(150, height - 200, 20);  // Создаем тыкву
        World.add(engine.world, pumpkin);  // Добавляем тыкву в мир

        slingshot = new SlingShot(150, height - 200, pumpkin);  // Создаем рогатку с тыквой
        Events.on(engine, "collisionStart", this::collision);  // Отслеживаем столкновения

        torch1 = new Torch(330, 620);  // Создаем факел 1
        torch2 = new Torch(1250, 455);  // Создаем факел 2

        explosionManager = new ExplosionManager();  // Создаем менеджер взрывов

        // Загружаем изображения
        titleScreen = loadImage("angry_pumpkins.jpg");
        imgSkull = loadImage("skull.png");
        pumpkinImg = loadImage("angry2.png");
        imgBoxSkull = loadImage("box2.png");
        imgStone1 = loadImage("stone2.png");
        imgBone1 = loadImage("bone1.png");
        imgPlank1 = loadImage("plank1.png");
        monsterImg = loadImage("monster.png");

        // Инициализируем игровые объекты (ящики)
        boxes.add(new GameObject(600, 650, 30, 100, imgBone1, 1.05f));
        boxes.add(new GameObject(600, 550, 30, 100, imgBone1, 1.05f));
        boxes.add(new GameObject(650, 600, 50, 50, monsterImg, 1.1f, true, true));
        boxes.add(new GameObject(700, 650, 30, 100, imgBone1, 1.05f));
        boxes.add(new GameObject(700, 550, 30, 100, imgBone1, 1.05f));
        boxes.add(new GameObject(650, 480, 150, 25, imgPlank1, 1.05f));
        boxes.add(new GameObject(650, 450, 70, 70, imgBoxSkull, 1.05f));
        boxes.add(new GameObject(650, 380, 50, 50, monsterImg, 1.1f, true, true));
        // Добавляем другие игровые объекты (ящики) здесь

        // Дополнительный код инициализации можно добавить здесь
    }

    public void draw() {
        if (!gameStarted) {
            image(titleScreen, 0, 0, width, height);  // Отображаем заставку
        } else {
            clear();
            Matter.Engine.update(engine);  // Обновляем механизм физической симуляции

            explosionManager.updateAndDisplay();  // Обновляем и отображаем взрывы

            if (pumpkinReleased && Math.abs(pumpkin.velocity.x) < 0.01 && Math.abs(pumpkin.velocity.y) < 0.01) {
                resetPumpkin();  // Сбрасываем тыкву, если она остановилась
            }

            // Устанавливаем режим смешивания ADD для эффекта смешивания
            blendMode(ADD);

            torch1.display();  // Отображаем факел 1
            torch2.display();  // Отображаем факел 2

            // Сбрасываем режим смешивания на BLEND для отображения остальных объектов
            blendMode(BLEND);

            slingshot.display();  // Отображаем рогатку

            PVector pumpkinPosition = new PVector(pumpkin.position.x, pumpkin.position.y);
            float angle;
            if (!pumpkinHasCollided) {
                if (!pumpkinReleased) {
                    angle = atan2(slingshot.origin.y - pumpkinPosition.y, slingshot.origin.x - pumpkinPosition.x);
                } else {
                    PVector velocity = new PVector(pumpkin.velocity.x, pumpkin.velocity.y);
                    angle = atan2(velocity.y, velocity.x);
                }
            } else {
                angle = pumpkin.angle();
            }

            pushMatrix();
            translate(pumpkinPosition.x, pumpkinPosition.y);
            rotate(angle);
            imageMode(CENTER);
            image(pumpkinImg, 0, 0, 40, 40);  // Отображаем тыкву
            popMatrix();

            for (GameObject box : boxes) {
                box.display();  // Отображаем игровые объекты (ящики)
            }
        }
    }

    public void mouseDragged() {
        float d = dist(mouseX, mouseY, pumpkin.position.x, pumpkin.position.y);
        if (!pumpkinReleased && d < interactRadius) {
            pumpkinBeingDragged = true;
            float stretchDistance = dist(mouseX, mouseY, slingshot.origin.x, slingshot.origin.y);
            if (stretchDistance > maxStretch) {
                float angle = atan2(mouseY - slingshot.origin.y, mouseX - slingshot.origin.x);
                float newPosX = slingshot.origin.x + maxStretch * cos(angle);
                float newPosY = slingshot.origin.y + maxStretch * sin(angle);
                Body.setPosition(pumpkin, new PVector(newPosX, newPosY));
            } else {
                Body.setPosition(pumpkin, new PVector(mouseX, mouseY));
            }
        }
    }

    public void mousePressed() {
        if (!gameStarted) {
            gameStarted = true;
        } else {
            if (keyPressed && (key == 'q' || key == 'Q')) {
                GameObject box = new GameObject(mouseX, mouseY, 70, 70, imgBoxSkull, 1.05f);
                boxes.add(box);  // Добавляем новый игровой объект (ящик) с изображением черепа
            }

            if (keyPressed && (key == 'w' || key == 'W')) {
                GameObject box = new GameObject(mouseX, mouseY, 20, 70, imgStone1, 1.05f);
                boxes.add(box);  // Добавляем новый игровой объект (ящик) с изображением камня
            }

            if (keyPressed && (key == 'e' || key == 'E')) {
                GameObject box = new GameObject(mouseX, mouseY, 30, 100, imgBone1, 1.05f);
                boxes.add(box);  // Добавляем новый игровой объект (ящик) с изображением кости
            }

            if (keyPressed && (key == 'r' || key == 'R')) {
                GameObject box = new GameObject(mouseX, mouseY, 150, 25, imgPlank1, 1.05f);
                boxes.add(box);  // Добавляем новый игровой объект (ящик) с изображением деревянной планки
            }
        }
    }

    public void mouseReleased() {
        if (pumpkinBeingDragged) {
            pumpkinReleased = true;
            float dx = slingshot.origin.x - pumpkin.position.x;
            float dy = slingshot.origin.y - pumpkin.position.y;
            PVector force = new PVector(dx, dy);
            force.mult(strength);
            Body.applyForce(pumpkin, pumpkin.position, force);
            slingshot.detachPumpkin();
            pumpkinBeingDragged = false;
        }
    }

    void collision(Events.CollisionEvent event) {
        for (int i = 0; i < event.pairs.length; i++) {
            Events.CollisionPair pair = event.pairs[i];
            Body bodyA = pair.bodyA;
            Body bodyB = pair.bodyB;

            if (bodyA == pumpkin || bodyB == pumpkin) {
                pumpkinHasCollided = true;
            }
        }
    }

    void resetPumpkin() {
        Body.setPosition(pumpkin, new PVector(150, height - 200));
        pumpkinReleased = false;
        pumpkinHasCollided = false;
        slingshot.attachPumpkin(pumpkin);
    }

    public static void main(String[] args) {
        PApplet.main("Main");
    }
}
