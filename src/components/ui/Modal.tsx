import React from 'react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
}

export const Modal: React.FC<ModalProps> = ({ 
  isOpen, 
  onClose, 
  children, 
  size = 'lg' 
}) => {
  if (!isOpen) return null;

  const sizeClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
    full: 'max-w-full w-full h-full'
  };

  return (
    <div className="fixed inset-0 z-50 overflow-auto">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
        onClick={onClose}
      />
      
      {/* Modal Content */}
      <div className={`relative min-h-screen flex items-center justify-center p-4 ${size === 'full' ? '' : 'py-12'}`}>
        <div className={`relative bg-white rounded-lg shadow-xl ${sizeClasses[size]} ${
          size === 'full' ? 'h-full' : 'max-h-[90vh]'
        }`}>
          {children}
        </div>
      </div>
    </div>
    
  );
};
