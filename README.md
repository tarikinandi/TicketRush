# TicketRush - High Concurrency Ticket Booking System  

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![React](https://img.shields.io/badge/React-18-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED)
![Redis](https://img.shields.io/badge/Redis-Atomic_Lock-red)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Messaging-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)

**TicketRush**, popüler konserler ve etkinlikler gibi yüksek talep gören senaryolarda **race condition** sorunlarını çözen, **event-driven** ve tam dağıtık bir bilet satış sistemidir.

Amaç, binlerce kullanıcının aynı anda “Satın Al” butonuna bastığı durumda bile:

- **Veri tutarlılığını** sağlamak  
- **Overselling** riskini ortadan kaldırmak  
- Sistemi kilitlemeden, ölçeklenebilir bir mimari ile süreci yönetmektir.  


---

## Teknoloji Yığını

Aşağıdaki teknoloji yığını, yüksek trafikli sistemlerin ihtiyaçları olan **ölçeklenebilirlik**, **tutarlılık** ve **performans** kriterlerine göre seçildi.

| Teknoloji                   | Kullanım Amacı                   | Neden Seçildi? |
| --------------------------- | -------------------------------- | -------------- |
| **Java 21 & Spring Boot 3** | Backend API                      | Kurumsal standart, yüksek performans, güçlü thread yönetimi ve geniş ekosistem desteği. |
| **React (Vite) + Tailwind** | Frontend UI                      | Hızlı geliştirme, modüler bileşen yapısı, WebSocket ile reaktif arayüz güncellemeleri. |
| **Redis (Lua Scripting)**   | Stok Yönetimi & Kilit            | Race condition’ı önlemek için bellek içi (in-memory) atomik işlemler (`DECR`, Lua script) ve milisaniyelik kilit mekanizması. |
| **RabbitMQ**                | Asenkron Mesajlaşma              | Yoğun istek anında veritabanını bottleneck olmaktan çıkarıp yükü kuyruğa alarak backpressure uygulayabilmek. |
| **PostgreSQL**              | Kalıcı Veri (Persistence)        | ACID uyumlu, ilişkisel veri bütünlüğü, güvenilir sipariş kaydı. |
| **WebSocket**       | Gerçek Zamanlı Bildirimler       | Sayfayı yenilemeden, long polling kullanmadan anlık stok ve işlem sonucu iletimi. |
| **Docker & Docker Compose** | Orkestrasyon ve Ortam İzolasyonu | Tüm bağımlılıkları (DB, Queue, Cache, App) tek komutla, ortam bağımsız ve izole çalıştırabilmek. |


---

## Temel Özellikler ve Çözülen Sorunlar

Bu bölümde, bir **Principal Architect** perspektifiyle sistemin kritik problemleri nasıl çözdüğünü anlatıyoruz.

### 1\. Race Condition & Concurrency Çözümü

Geleneksel yaklaşım:

`SELECT stock FROM tickets WHERE id = ?; IF stock > 0 THEN     UPDATE tickets SET stock = stock - 1 WHERE id = ?; END IF;`

Yüksek trafikte aynı satır için birden fazla işlem yarıştığında:

-   Aynı koltuk/bilet birden fazla kişiye satılabilir.
    
-   Veritabanı lock/transaction baskısı artar.
    

**Çözüm:**

-   **Redis + Lua Script** ile “stok kontrolü” ve “stok düşme” işlemi **atomik tek işlem** haline getirildi.
    
-   Redis tek-thread çalıştığı için aynı anda gelen isteklerde bile komutlar sıraya girer.
    

Basit akış:

1.  API isteği gelir.
    
2.  Redis üzerinde Lua script çalışır:
    
    -   `if stock > 0 then stock-- else return SOLD_OUT end`
        
3.  Script **ya tamamen başarılı olur ya tamamen başarısız olur**.
    

**Sonuç:**

-   Saniyede 1000+ istek gelse bile:
    
    -   **Asla stoktan fazla satış yapılmaz.**
        
    -   Veritabanı üzerinde transaction yükü azaltılır.
        
    -   Kritik karar “en uçta” ve bellek üzerinde alınır.
        

* * *

### 2\. Gatekeeper Pattern

Sistemi korumak için ilk savunma hattı **stok seviyesi**dir.

-   Stok yoksa istek **anında reddedilir**.
    
-   Veritabanı, RabbitMQ ve diğer servisler gereksiz yükten kurtulur.
    
-   Uygulama sadece “kabul edilebilir” iş yükünü core servislere iletir.
    

Akış:

1.  İstek geldiğinde hemen Redis’ten stok düşme denenir.
    
2.  Sonuç:
    
    -   Başarılı → Event kuyruğa yazılır, sipariş süreci devam eder.
        
    -   Başarısız → Anında `HTTP 409` / `400` benzeri iş kuralı hatası döner.
        

Bu sayede:

-   DDoS benzeri yüksek trafiklerde bile sistemin “çekirdeği” korunur.
    
-   Kuyruklar ve veritabanı sadece anlamlı istekleri görür.
    

* * *

### 3\. Asenkron Sipariş İşleme 

Kritik karar: **Kullanıcı “Satın Al” dediğinde, sipariş senkron olarak veritabanına yazılmaz.**

Bunun yerine:

1.  Stok kontrolü Redis’te geçerse:
    
    -   Sipariş isteği **RabbitMQ Exchange**’ine event olarak publish edilir.
        
2.  Queue → Consumer:
    
    -   Kuyruktan mesajı çeker.
        
    -   Sipariş kaydını PostgreSQL’e yazar.
        
3.  İşlem tamamlandığında:
    
    -   Kullanıcıya WebSocket üzerinden “başarılı” bildirimi gönderilir.
        

**Kazançlar:**

-   API yanıt süresi kısalır.
    
-   Worker sayıları yatayda ölçeklenebilir.
    
-   **Backpressure**: Veritabanı kapasitesi kadar iş işlenir; gerisi kuyrukta bekler.
    

* * *

### 4\. Gerçek Zamanlı Bildirimler

Kullanıcı deneyimi açısından:

-   Kullanıcı, butona bastıktan sonra sürekli sayfa yenilemek zorunda kalmaz.
    
-   **WebSocket + STOMP** ile:
    
    -   “Siparişiniz alındı”
        
    -   “Stok tükendi”
        
    -   “İşleminiz onaylandı / başarısız oldu”
        

gibi durumlar **anlık** olarak UI’da güncellenir.

Bu sayede:

-   Hem backend event-driven, hem de frontend reaktif bir yapıda çalışır.

## Kurulum ve Çalıştırma

## 

Projeyi yerel ortamda çalıştırmak için **yalnızca Docker yüklü olması** yeterlidir.

### 1\. Depoyu Klonlayın

## 

```git clone https://github.com/tarikinandi/TicketRush.git```

### 2\. Tüm Sistemi Docker Compose ile Başlatın

## 

Aşağıdaki komut, aşağıdaki bileşenleri ayağa kaldırır:

-   PostgreSQL
    
-   Redis
    
-   RabbitMQ
    
-   TicketRush Backend (Spring Boot)
    
-   TicketRush Frontend (React)
    

```docker-compose up --build```

### 3\. Servis Adresleri

## 

-   **Frontend:** `http://localhost:5173`
    
-   **Backend API:** `http://localhost:8080`
    
-   **RabbitMQ Management Panel:** `http://localhost:15672`
    
    -   Kullanıcı: `admin`
        
    -   Şifre: `password`


---

## Load Testing ve Kanıtlar

Sistemin dayanıklılığını ve race condition senaryolarındaki davranışını göstermek için, proje içerisinde Node.js tabanlı bir yük testi script’i bulunmaktadır: load-test.js

## Test Senaryosu

 Mevcut Stok: 10 adet

 Eşzamanlı İstek: 100 adet 

### Beklenen Sonuç:

 10 adet başarılı bilet satışı

 90 adet “Stok Tükendi” cevabı

 0 adet sunucu hatası (500, crash vs.)

## Testin Çalıştırılması

Backend ve diğer servisler Docker üzerinden ayaktayken:

``` node load-test.js```

## Örnek Test Çıktısı

TICKETRUSH YÜK TESTİ BAŞLIYOR...

Hedef: [http://localhost:8080/api/tickets/buy](http://localhost:8080/api/tickets/buy)

Toplam İstek: 100

----------------------------------------

#### [SUCCESS] User 8189 bilet aldı! (173ms)

... (Total 10 Success Logs) ...

#### [SOLD OUT] User 7409 stok bitimine takıldı. (386ms)

... (Total 90 Sold Out Logs) ...

----------------------------------------

## TEST SONUÇLARI:

Başarılı Satış (Stok düştü): 10

Başarıyla Engellenen (Stok bitti): 90

Hatalar (Crash/Bug): 0

#### MÜKEMMEL: Sistem tam beklendiği gibi davrandı!

#### Bu sonuç, sistemin: Tutarlı, Dayanıklı, Backpressure uygulayan bir yapıda çalıştığını gösterir.


---

## Proje Yapısı

Genel klasör yapısı:

### TicketRush

├── ticketrush-backend/          # Spring Boot Uygulaması

│   ├── src/main/java/com/ticketrush/

│   │   ├── config/              # RabbitMQ, Redis, WebSocket Konfigürasyonları

│   │   ├── controller/          # REST Endpoint'ler

│   │   ├── service/             # İş Mantığı (Producer, Redis Service)

│   │   ├── listener/            # RabbitMQ Consumer

│   │   └── exception/           # Global Hata Yönetimi

│   └── Dockerfile

│

├── ticketrush-ui/               # React Uygulaması

│   ├── src/components/          # Dashboard ve UI Bileşenleri

│   ├── src/services/            # API İstekleri (Axios)

│   └── Dockerfile

│

├── docker-compose.yml           # Orkestrasyon Dosyası

└── load-test.js                 # Yük Testi Script'i


---

## İletişim

Bu proje, yüksek ölçekli sistem mimarileri ve yüksek eşzamanlılık senaryoları durumlarını test etmek için hazırlanmıştır.

- Geliştirici: Tarık İnandı

- LinkedIn: [https://www.linkedin.com/in/tarikinandi/](https://www.linkedin.com/in/tarikinandi/)

- Email: [inandi.tarik@gmail.com](mailto:inandi.tarik@gmail.com)