import time
from fastapi import FastAPI
from fastapi.responses import JSONResponse

app = FastAPI(title="AI Planner Service", version="1.0.0")

_START_TIME = time.time()


@app.get("/health")
def health():
    """Nacos health check endpoint"""
    return JSONResponse({
        "status": "UP",
        "service": "ai-planner-service",
        "uptime_seconds": round(time.time() - _START_TIME, 1),
    })


@app.get("/info")
def info():
    """Service metadata"""
    from app.registry.nacos import SERVICE_NAME, LOCAL_IP, SERVICE_PORT
    return JSONResponse({
        "service": SERVICE_NAME,
        "host": LOCAL_IP,
        "port": SERVICE_PORT,
        "version": "1.0.0",
    })


from app.api.chat import router as chat_router
app.include_router(chat_router)
