const API_URL = "http://localhost:8080/api/tickets/buy"; 

const TOTAL_REQUESTS = 100; 
const CONCURRENCY_DELAY = 10; 

const colors = {
    reset: "\x1b[0m",
    green: "\x1b[32m",
    red: "\x1b[31m",
    yellow: "\x1b[33m",
    cyan: "\x1b[36m"
};

async function sendRequest(id) {
    const userId = Math.floor(Math.random() * 10000) + 1;
    const body = JSON.stringify({
        userId: userId,
        amount: 1,
        eventId: 1
    });

    try {
        const start = Date.now();
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: body
        });
        const duration = Date.now() - start;

        if (response.ok) {
            console.log(`${colors.green}✔ [SUCCESS] User ${userId} bilet aldı! (${duration}ms)${colors.reset}`);
            return { status: "success", duration };
        } else {
            let errorMsg = "";
            try {
                const json = await response.json();
                errorMsg = json.message || JSON.stringify(json);
            } catch (e) {
                errorMsg = await response.text();
            }

            if (response.status === 400) {
                console.log(`${colors.yellow}⚠ [SOLD OUT] User ${userId} stok bitimine takıldı. (${duration}ms)${colors.reset}`);
                return { status: "sold_out", duration };
            } else {
                console.log(`${colors.red}✖ [ERROR] User ${userId} hata aldı: ${response.status} - ${errorMsg} (${duration}ms)${colors.reset}`);
                return { status: "error", code: response.status, duration };
            }
        }
        
    } catch (error) {
        console.log(`${colors.red}✖ [NETWORK FAIL] Bağlantı hatası! Server çökmüş olabilir.${colors.reset}`);
        return { status: "network_error", error: error.message };
    }
}

async function runLoadTest() {
    console.log(`${colors.cyan}🚀 TICKETRUSH YÜK TESTİ BAŞLIYOR...${colors.reset}`);
    console.log(`Hedef: ${API_URL}`);
    console.log(`Toplam İstek: ${TOTAL_REQUESTS}`);
    console.log(`----------------------------------------`);

    const promises = [];
    
    for (let i = 0; i < TOTAL_REQUESTS; i++) {
        const p = new Promise(resolve => {
            setTimeout(() => {
                sendRequest(i).then(resolve);
            }, i * CONCURRENCY_DELAY); 
        });
        promises.push(p);
    }

    const results = await Promise.all(promises);

    console.log(`----------------------------------------`);
    console.log(`${colors.cyan}📊 TEST SONUÇLARI:${colors.reset}`);
    
    const successCount = results.filter(r => r.status === "success").length;
    const soldOutCount = results.filter(r => r.status === "sold_out").length;
    const errorCount = results.filter(r => r.status === "error" || r.status === "network_error").length;

    console.log(`${colors.green}✅ Başarılı Satış (Stok düştü): ${successCount}${colors.reset}`);
    console.log(`${colors.yellow}🛡️ Başarıyla Engellenen (Stok bitti): ${soldOutCount}${colors.reset}`);
    console.log(`${colors.red}💀 Hatalar (Crash/Bug): ${errorCount}${colors.reset}`);

    if (successCount > 10) {
        console.log(`\n${colors.red}❌ KRİTİK HATA: Stoktan fazla bilet satıldı! (Race Condition Var)${colors.reset}`);
    } else if (successCount + soldOutCount === TOTAL_REQUESTS) {
        console.log(`\n${colors.green}🏆 MÜKEMMEL: Sistem tam beklendiği gibi davrandı!${colors.reset}`);
    } else {
        console.log(`\n${colors.red}⚠️ Uyarı: Beklenmeyen hata oranları var. Logları kontrol et.${colors.reset}`);
    }
}

runLoadTest();