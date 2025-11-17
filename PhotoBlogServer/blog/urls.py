from django.urls import path, include
from rest_framework import routers
from .views import BlogImages

router = routers.DefaultRouter()
router.register('Post', BlogImages)

urlpatterns = [
    path('api_root/', include(router.urls)),
]