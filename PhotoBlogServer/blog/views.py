from rest_framework import viewsets
from .models import Post
from .serializers import PostSerializer
from django.contrib.auth.models import User
from .serializers import UserSerializer

class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all().order_by('-id')
    serializer_class = PostSerializer

class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer