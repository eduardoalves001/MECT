document.addEventListener('DOMContentLoaded', function() {
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        const passwordGroup = passwordInput.closest('.form-group');
        
        const toggleBtn = document.createElement('button');
        toggleBtn.type = 'button';
        toggleBtn.className = 'password-toggle';
        toggleBtn.setAttribute('aria-label', 'Toggle password visibility');
        toggleBtn.textContent = '';
        
        passwordGroup.style.position = 'relative';
        passwordGroup.appendChild(toggleBtn);
        
        toggleBtn.addEventListener('click', function() {
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                toggleBtn.classList.add('visible');
            } else {
                passwordInput.type = 'password';
                toggleBtn.classList.remove('visible');
            }
        });
    }
});
